package se.kth.id2203.protocols.sequence_consencus;

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Send, PerfectLinkPort}
import se.kth.id2203.protocols.ble.{BLE_Leader, BallotLeaderElectionPort}
import se.kth.id2203.kvstore.DecidedOperation;
import se.kth.id2203.utils.{Leader};
class SequencePaxos(init: Init[SequencePaxos]) extends ComponentDefinition {
  //********** Init **********
  import Role._;
  import State._;

  val self: NetAddress = cfg.getValue[NetAddress]("id2203.project.address");
  val (pi, others, groupIndex) = init match {
    case Init(topology: Set[NetAddress] @unchecked, groupIndex: Int) => (topology, topology - self, groupIndex)
    case _                                                           => (Set(self), Set.empty[NetAddress], -1)
  }
  val majority: Int        = ((pi.size + 1) / 2f).ceil.toInt;
  val bootstrapServer: NetAddress = cfg.getValue[NetAddress]("id2203.project.bootstrap-address");
  //********** Subscriptions **********
  val pLink: PositivePort[PerfectLinkPort] = requires[PerfectLinkPort]
  val ble: PositivePort[BallotLeaderElectionPort]   = requires[BallotLeaderElectionPort];
  val sc: NegativePort[SequenceConsensusPort]    = provides[SequenceConsensusPort];

  //********** Internal state **********
  var state: (Role.Value, State.Value)                      = (FOLLOWER, UNKNOWN);
  var nL                         = 0L;
  var nProm                      = 0L;
  var leader: Option[NetAddress] = None;
  var na                         = 0L;
  var va: List[RSM_Command]                         = Nil;
  var ld                         = 0;

  //********** Leader state **********
  var propCmds: List[RSM_Command] = Nil;
  var las: Map[NetAddress,Int]      = Map.empty[NetAddress, Int];
  var lds: Map[NetAddress,Int]      = Map.empty[NetAddress, Int];
  var lc       = 0;
  var acks: Map[NetAddress,(Long, List[RSM_Command])]     = Map.empty[NetAddress, (Long, List[RSM_Command])];

  //********** Helpers **********
  def suffix(s: List[RSM_Command], l: Int): List[RSM_Command] = {
    s.drop(l);
  }

  def prefix(s: List[RSM_Command], l: Int): List[RSM_Command] = {
    s.take(l);
  }

  def notifyLeadership(): Unit = {
    trigger(PL_Send(bootstrapServer, Leader(self, groupIndex)) -> pLink);
  }

  //********** Handlers **********
  ble uponEvent {
    case BLE_Leader(l, n) => {
      if (n > nL) {
        log.info(s"[$self] proposing leader $l");
        leader = Some(l);
        nL = n;
        if (self == l && nL > nProm) {
          log.info(s"[$self] declaring myself the leader")
          state = (LEADER, PREPARE);
          /* Notify our leadership */
          notifyLeadership();
          /* Clear everything */
          propCmds = List.empty;
          las = Map.empty;
          lds = Map.empty;
          acks = Map.empty;
          lc = 0;

          for (p <- others) {
            trigger(PL_Send(p, Prepare(nL, ld, na)) -> pLink);
          }
          /* Set our new state */
          acks = acks + (l -> (na, suffix(va, ld)));
          lds += (self     -> ld);
          nProm = nL;
        }
      } else {
        state = (FOLLOWER, state._2)
      }
    }
  }

  pLink uponEvent {
    case PL_Deliver(p, Prepare(np, ldp @ _, n)) => {
      if (nProm < np) {
        log.info(s"[$self] Follower preparing")
        nProm = np;
        state = (FOLLOWER, PREPARE);

        val sfx = if (na >= n) suffix(va, ld) else List.empty;
        trigger(PL_Send(p, Promise(np, na, sfx, ld)) -> pLink)
      }
    }
    case PL_Deliver(a, Promise(n, na, sfxa, lda)) => {
      log.info(s"$a promises to follow the leader $self");
      if ((n == nL) && (state == (LEADER, PREPARE))) {
        acks += (a -> (na, sfxa));
        lds += (a  -> lda);
        if (acks.size == majority) {
          val sfx = acks.values.maxBy(_._1)._2;

          va = prefix(va, ld) ++ sfx ++ propCmds;
          las += (self -> va.size);

          propCmds = List.empty;
          state = (LEADER, ACCEPT);
          others
            .filter(lds.contains(_))
            .foreach(p => {
              val sfxp = suffix(va, lds(p));
              trigger(PL_Send(p, AcceptSync(nL, sfxp, lds(p))) -> pLink);
            })
        }
      } else if ((n == nL) && (state == (LEADER, ACCEPT))) {
        lds += (a -> lda);
        val sfx = suffix(va, lds(a));
        trigger(PL_Send(a, AcceptSync(nL, sfx, lds(a))) -> pLink);
        if (lc != 0) {
          trigger(PL_Send(a, Decide(ld, nL)) -> pLink);
        }
      }
    }
    case PL_Deliver(p, AcceptSync(nL, sfx, ldp @ _)) => {
      if ((nProm == nL) && (state == (FOLLOWER, PREPARE))) {
        na = nL;
        va = prefix(va, ld) ++ sfx;
        trigger(PL_Send(p, Accepted(nL, va.size)) -> pLink);
        state = (FOLLOWER, ACCEPT);
      }
    }
    case PL_Deliver(p, Accept(nL, c)) => {
      if ((nProm == nL) && (state == (FOLLOWER, ACCEPT))) {
        va = va :+ c;
        trigger(PL_Send(p, Accepted(nL, va.size)) -> pLink);
      }
    }
    case PL_Deliver(_, Decide(l, nL)) => {
      if (nProm == nL) {
        while (ld < l) {
          //log.info(s"[$self] decided value ${va(ld)}")
          trigger(SC_Decide(DecidedOperation(va(ld), state._1)) -> sc);
          ld += 1;
        }
      }
    }
    case PL_Deliver(a, Accepted(n, m)) => {
      if ((n == nL) && (state == (LEADER, ACCEPT))) {
        las += (a -> m);
        if (lc < m && pi.count(p => las.contains(p) && las(p) >= m) >= majority) {
          lc = m;

          pi.filter(lds.contains(_))
            .foreach(p => {
              trigger(PL_Send(p, Decide(lc, nL)) -> pLink)
            })
        }
      }
    }
  }

  sc uponEvent {
    case SC_Propose(c) => {
      if (state == (LEADER, PREPARE)) {
        log.info(s"[$self] Leader received command: $c");
        propCmds = propCmds :+ c;
      } else if (state == (LEADER, ACCEPT)) {
        log.info(s"[$self] Leader accepts command: $c");
        va = va :+ c;
        las = las + (self -> (las(self) + 1));
        others
          .filter(lds.contains(_))
          .foreach(p => {
            trigger(PL_Send(p, Accept(nL, c)) -> pLink)
          })
      }
    }
  }
}

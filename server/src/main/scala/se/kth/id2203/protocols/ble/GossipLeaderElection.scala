package se.kth.id2203.protocols.ble

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.sics.kompics.timer.ScheduleTimeout
import se.sics.kompics.timer.Timer
import se.kth.id2203.protocols.perfect_link.PerfectLinkPort
import com.google.common.primitives.{Ints, Longs}
import se.kth.id2203.protocols.perfect_link.PL_Send
import se.kth.id2203.protocols.perfect_link.PL_Deliver

/** Implementation of Ballot Leader Election using the gossip leader election algorithm
  */
class GossipLeaderElection(init: Init[GossipLeaderElection]) extends ComponentDefinition {
  //****** Init ******
  val self: NetAddress = cfg.getValue[NetAddress]("id2203.project.address");
  val topology: Set[NetAddress] = init match {
    case Init(topology: Set[NetAddress] @unchecked) => topology
    case _                                          => Set.empty[NetAddress]
  }
  val delta: Long     = cfg.getValue[Long]("id2203.project.epfd.delay");
  val ballotOne = 0x0100000000L

  //****** Subscriptions ******
  val timer: PositivePort[Timer] = requires[Timer];
  val pLink: PositivePort[PerfectLinkPort] = requires[PerfectLinkPort];
  val ble: NegativePort[BallotLeaderElectionPort]   = provides[BallotLeaderElectionPort];
  //****** Variables ******
  var round                              = 0;
  var ballots: Map[NetAddress,Long]                            = Map.empty[NetAddress, Long];
  var leader: Option[(Long, NetAddress)] = None;

  var ballot: Long    = ballotFromNetworkAddress(0, self);
  var period    = delta;
  var maxBallot = ballot;

  def increment(ballot: Long): Long = {
    ballot + ballotOne;
  }

  def ballotFromNetworkAddress(n: Int, address: NetAddress): Long = {
    val nBytes    = Ints.toByteArray(n);
    val addrBytes = Ints.toByteArray(address.hashCode()); val bytes = nBytes ++ addrBytes;
    val r         = Longs.fromByteArray(bytes);
    assert(r > 0); // should not produce negative numbers!
    r
  }

  def startTimer(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(delay);
    scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout))
    trigger(scheduledTimeout -> timer)
  }

  def checkLeader(): Unit = {
    val (topProcess, topBallot) = (ballots + (self -> ballot)).maxBy(_._2);
    val top                     = (topBallot, topProcess);
    if (topBallot < maxBallot) {
      while (ballot <= maxBallot) {
        ballot = increment(ballot)
      }
      leader = None;
    } else if (leader.isEmpty || top != leader.get) {
      maxBallot = topBallot;
      leader = Some(top);
      trigger(BLE_Leader(topProcess, topBallot) -> ble)
    }
  }

  ctrl uponEvent {
    case _: Start => {
      startTimer(period)
    }
  }

  timer uponEvent {
    case CheckTimeout(_) => {
      if (ballots.size + 1 >= (topology.size / 2f).ceil.toInt) {
        checkLeader();
      }
      ballots = Map.empty;
      round += 1;
      for (p <- topology) {
        trigger(PL_Send(p, HeartbeatRequest(round, maxBallot)) -> pLink);
      }
      startTimer(period);
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, HeartbeatRequest(r, bMax)) => {
      if (bMax > maxBallot) {
        maxBallot = bMax;
      }
      trigger(PL_Send(src, HeartbeatReply(r, ballot)) -> pLink);
    }
    case PL_Deliver(src, HeartbeatReply(r, b)) => {
      if (r == round) {
        ballots += (src -> b);
      } else {
        period += delta;
      }
    }
  }
}

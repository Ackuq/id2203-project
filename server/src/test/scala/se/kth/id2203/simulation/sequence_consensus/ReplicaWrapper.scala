package se.kth.id2203.simulation.sequence_consensus;

import se.sics.kompics.sl._;
import se.kth.id2203.protocols.perfect_link.PerfectLinkPort;

import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.network.Network;
import se.kth.id2203.bootstrapping.Bootstrapping
import se.kth.id2203.bootstrapping.Booted
import se.kth.id2203.overlay.LookupTable
import se.kth.id2203.protocols.ble.GossipLeaderElection
import se.sics.kompics.timer.Timer
import se.kth.id2203.protocols.sequence_consencus.SequencePaxos
import se.kth.id2203.protocols.ble.BallotLeaderElectionPort
import se.kth.id2203.protocols.sequence_consencus.SequenceConsensusPort
import se.kth.id2203.networking.NetAddress

class ReplicaWrapper extends ComponentDefinition {
  val self  = cfg.getValue[NetAddress]("id2203.project.address");
  val boot  = requires(Bootstrapping);
  val pLink = requires[PerfectLinkPort];
  val net   = requires[Network];
  val timer = requires[Timer];

  val kv = create(classOf[KVService], Init.NONE);

  boot uponEvent {
    case Booted(assignment: LookupTable) => {
      try {
        val (key, partition) = assignment.getPartition(self);

        val ble     = create(classOf[GossipLeaderElection], Init[GossipLeaderElection](partition.toSet));
        val seqCons = create(classOf[SequencePaxos], Init[SequencePaxos](partition.toSet, key));
        val seqConsMiddleware =
          create(classOf[SequentialConsensusMiddleware], Init[SequentialConsensusMiddleware](key));

        trigger(new Start() -> ble.control());
        trigger(new Start() -> seqCons.control());
        trigger(new Start() -> seqConsMiddleware.control());

        // (Gossip) Ballot Leader Election
        connect[PerfectLinkPort](pLink -> ble);
        connect[Timer](timer           -> ble);

        // Sequence Paxos
        connect[PerfectLinkPort](pLink        -> seqCons);
        connect[BallotLeaderElectionPort](ble -> seqCons)

        // Scenario client
        connect[SequenceConsensusPort](seqCons -> seqConsMiddleware);

        // KV
        connect[Network](net                             -> kv);
        connect[PerfectLinkPort](pLink                   -> kv);
        connect[SequenceConsensusPort](seqConsMiddleware -> kv);

      } catch {
        case e: IllegalArgumentException => {
          log.warn(s"Got message whe starting KV-Store: ${e.getMessage()}")
        }
      }
    }
  }
}

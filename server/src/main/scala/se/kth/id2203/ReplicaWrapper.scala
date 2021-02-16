package se.kth.id2203

import se.sics.kompics.sl._;
import se.kth.id2203.protocols.beb.BestEffortBroadcast;
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

class ReplicaWrapper extends ComponentDefinition {
  val boot  = requires(Bootstrapping);
  val pLink = requires[PerfectLinkPort];
  val net   = requires[Network];
  val timer = requires[Timer];

  val kv = create(classOf[KVService], Init.NONE);

  boot uponEvent {
    case Booted(assignment: LookupTable) => {
      val topology = assignment.getNodes();

      val beb     = create(classOf[BestEffortBroadcast], Init[BestEffortBroadcast](topology));
      val ble     = create(classOf[GossipLeaderElection], Init[GossipLeaderElection](topology));
      val seqCons = create(classOf[SequencePaxos], Init[SequencePaxos](topology));

      trigger(new Start() -> beb.control());
      trigger(new Start() -> ble.control());
      trigger(new Start() -> seqCons.control());

      // Best Effort Broadcast
      connect[PerfectLinkPort](pLink -> beb);

      // (Gossip) Ballot Leader Election
      connect[PerfectLinkPort](pLink -> ble);
      connect[Timer](timer           -> ble);

      // Sequence Paxos
      connect[PerfectLinkPort](pLink        -> seqCons);
      connect[BallotLeaderElectionPort](ble -> seqCons)

      // KV
      connect[Network](net                   -> kv);
      connect[PerfectLinkPort](pLink         -> kv);
      connect[SequenceConsensusPort](seqCons -> kv);
    }
  }
}

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

class ReplicaWrapper extends ComponentDefinition {
  val boot  = requires(Bootstrapping);
  val pLink = requires[PerfectLinkPort];
  val net   = requires[Network];
  val timer = requires[Timer];

  val kv = create(classOf[KVService], Init.NONE);

  boot uponEvent {
    case Booted(assignment: LookupTable) => {
      val topology = assignment.getNodes();

      // Best Effort Broadcast
      val beb = create(classOf[BestEffortBroadcast], Init[BestEffortBroadcast](topology));
      connect[PerfectLinkPort](pLink -> beb);

      // (Gossip) Ballot Leader Election
      val ble = create(classOf[GossipLeaderElection], Init[GossipLeaderElection](topology));
      connect[PerfectLinkPort](pLink -> ble);
      connect[Timer](timer           -> ble);

      // KV
      connect[Network](net           -> kv);
      connect[PerfectLinkPort](pLink -> kv);
    }
  }
}

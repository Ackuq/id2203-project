package se.kth.id2203.protocols.beb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Send, PerfectLinkPort}
import se.kth.id2203.overlay.LookupTable;
import se.kth.id2203.utils.{GROUP};

/** Implementation of best effort broadcast
  */
class BestEffortBroadcast extends ComponentDefinition {
  val self = cfg.getValue[NetAddress]("id2203.project.address");

  val pLink   = requires[PerfectLinkPort];
  val bebPort = provides[BestEffortBroadcastPort]

  var replicationGroup = Set.empty[NetAddress];

  bebPort uponEvent {
    case x @ BEB_Broadcast(_) => {
      for (p <- replicationGroup) {
        trigger(PL_Send(p, x) -> pLink)
      }
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, BEB_Broadcast(payload)) => {
      trigger(
        BEB_Deliver(src, payload) -> bebPort
      )
    }
    case PL_Deliver(self, GROUP(group)) => {
      // Update our group
      replicationGroup = group;
    }
  }
}

package se.kth.id2203.broadcast.beb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Send, PerfectLinkPort}

/** Implementation of best effort broadcast
  *
  * TODO: Be able to determine topology (group membership)
  */
class BestEffortBroadcast extends ComponentDefinition {
  val self = cfg.getValue[NetAddress]("id2203.project.address");

  val pLink   = requires[PerfectLinkPort];
  val bebPort = provides[BestEffortBroadcastPort]

  // Find good way to determine topology
  val topology = Set.empty[NetAddress];

  bebPort uponEvent {
    case x: BEB_Broadcast => {
      for (p <- topology) {
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
  }
}

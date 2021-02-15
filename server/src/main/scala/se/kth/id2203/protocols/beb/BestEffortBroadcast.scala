package se.kth.id2203.protocols.beb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Forward, PL_Send, PerfectLinkPort}
import se.kth.id2203.overlay.LookupTable;

/** Implementation of best effort broadcast
  */
class BestEffortBroadcast extends ComponentDefinition {
  val self = cfg.getValue[NetAddress]("id2203.project.address");

  val pLink   = requires[PerfectLinkPort];
  val bebPort = provides[BestEffortBroadcastPort]

  bebPort uponEvent {
    case x @ BEB_Broadcast(nodes, _) => {
      for (p <- nodes) {
        trigger(PL_Send(p, x) -> pLink)
      }
    }
    case x @ BEB_Broadcast_Forward(nodes, src, _) => {
      for (p <- nodes) {
        trigger(PL_Forward(src, p, x) -> pLink)
      }
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, BEB_Broadcast(_, payload)) => {
      trigger(
        BEB_Deliver(src, payload) -> bebPort
      )
    }
    case PL_Deliver(src, BEB_Broadcast_Forward(_, org, payload)) => {
      trigger(
        BEB_Deliver(src, payload) -> bebPort
      )
    }
  }
}

package se.kth.id2203.protocols.beb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Forward, PL_Send, PerfectLinkPort}
import se.kth.id2203.overlay.LookupTable;

/** Implementation of best effort broadcast
  */
class BestEffortBroadcast(init: Init[BestEffortBroadcast]) extends ComponentDefinition {
  val self = cfg.getValue[NetAddress]("id2203.project.address");

  val topology = init match {
    case Init(topology: Set[NetAddress] @unchecked) => topology
    case _                                          => Set.empty[NetAddress]
  }

  val pLink   = requires[PerfectLinkPort];
  val bebPort = provides[BestEffortBroadcastPort]

  bebPort uponEvent {
    case x @ BEB_Broadcast(_) => {
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

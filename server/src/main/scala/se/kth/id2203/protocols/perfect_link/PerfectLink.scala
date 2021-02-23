package se.kth.id2203.protocols.perfect_link

import se.kth.id2203.networking.NetAddress

import se.sics.kompics.sl._;
import se.sics.kompics.network.Network
import se.kth.id2203.networking.NetMessage
import se.kth.id2203.networking.NetHeader

/** Simple implementation of perfect link.
  * Don't know if this will be used, but good to have
  */
class PerfectLink extends ComponentDefinition {
  val self: NetAddress                     = cfg.getValue[NetAddress]("id2203.project.address");
  val pLink: NegativePort[PerfectLinkPort] = provides[PerfectLinkPort]
  val net: PositivePort[Network]           = requires[Network]

  pLink uponEvent {
    case PL_Send(dst: NetAddress, payload: KompicsEvent) => {
      trigger(NetMessage(self, dst, payload) -> net)
    }
    case PL_Forward(src: NetAddress, dst: NetAddress, payload: KompicsEvent) => {
      trigger(NetMessage(src, dst, payload) -> net);
    }
  }

  net uponEvent {
    case NetMessage(header: NetHeader, payload: KompicsEvent) => {
      trigger(PL_Deliver(header.src, payload) -> pLink)
    }
  }
}

package se.kth.id2203.protocols.beb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress;

case class BEB_Deliver(source: NetAddress, payload: KompicsEvent)                                extends KompicsEvent;
case class BEB_Broadcast(nodes: Set[NetAddress], payload: KompicsEvent)                          extends KompicsEvent;
case class BEB_Broadcast_Forward(nodes: Set[NetAddress], src: NetAddress, payload: KompicsEvent) extends KompicsEvent;

package se.kth.id2203.protocols.perfect_link

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress;

case class PL_Deliver(source: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class PL_Send(source: NetAddress, payload: KompicsEvent)    extends KompicsEvent;

package se.kth.id2203.broadcast.beb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress;

case class BEB_Deliver(source: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class BEB_Broadcast(payload: KompicsEvent)                   extends KompicsEvent;

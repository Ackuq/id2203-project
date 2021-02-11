package se.kth.id2203.protocols.rb

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress;

case class RB_Deliver(source: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class RB_Broadcast(payload: KompicsEvent)                   extends KompicsEvent;

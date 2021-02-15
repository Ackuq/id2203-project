package se.kth.id2203.protocols.epfd

import se.kth.id2203.networking.NetAddress

import se.sics.kompics.sl._;
import se.sics.kompics.timer.{ScheduleTimeout, Timeout}

case class Suspect(src: NetAddress) extends KompicsEvent;
case class Restore(src: NetAddress) extends KompicsEvent;

case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout);

case class HeartbeatReply(seq: Int)   extends KompicsEvent;
case class HeartbeatRequest(seq: Int) extends KompicsEvent

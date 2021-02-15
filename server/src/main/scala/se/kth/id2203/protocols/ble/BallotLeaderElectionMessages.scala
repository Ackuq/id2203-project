package se.kth.id2203.protocols.ble

import se.sics.kompics.sl._;
import se.sics.kompics.timer.{ScheduleTimeout, Timeout}
import se.kth.id2203.networking.NetAddress

case class BLE_Leader(leader: NetAddress, ballot: Long) extends KompicsEvent;

case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout);

case class HeartbeatRequest(seq: Int, maxBallot: Long) extends KompicsEvent;
case class HeartbeatReply(seq: Int, ballot: Long)      extends KompicsEvent;

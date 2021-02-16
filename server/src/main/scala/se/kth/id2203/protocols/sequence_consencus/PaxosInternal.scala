package se.kth.id2203.protocols.sequence_consencus;

import se.sics.kompics.sl._;

case class Prepare(nL: Long, ld: Int, na: Long)                            extends KompicsEvent;
case class Promise(nL: Long, na: Long, suffix: List[RSM_Command], ld: Int) extends KompicsEvent;
case class AcceptSync(nL: Long, suffix: List[RSM_Command], ld: Int)        extends KompicsEvent;
case class Accept(nL: Long, c: RSM_Command)                                extends KompicsEvent;
case class Accepted(nL: Long, m: Int)                                      extends KompicsEvent
case class Decide(ld: Int, nL: Long)                                       extends KompicsEvent;

object State extends Enumeration {
  type State = Value;
  val PREPARE, ACCEPT, UNKNOWN = Value;
}

object Role extends Enumeration {
  type Role = Value;
  val LEADER, FOLLOWER = Value;
}

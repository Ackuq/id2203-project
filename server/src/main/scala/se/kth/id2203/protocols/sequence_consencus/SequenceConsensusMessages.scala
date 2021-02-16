package se.kth.id2203.protocols.sequence_consencus;

import se.sics.kompics.sl._;

case class SC_Propose(value: RSM_Command) extends KompicsEvent;
case class SC_Decide(value: RSM_Command)  extends KompicsEvent;

trait RSM_Command;

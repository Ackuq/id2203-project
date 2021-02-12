package se.kth.id2203.kvstore

import se.sics.kompics.sl._;

case class READ(rid: Int)                                  extends KompicsEvent;
case class VALUE(rid: Int, ts: Int, value: Option[String]) extends KompicsEvent;
case class WRITE(rid: Int, ts: Int, value: Option[String]) extends KompicsEvent;
case class ACK(rid: Int)                                   extends KompicsEvent;

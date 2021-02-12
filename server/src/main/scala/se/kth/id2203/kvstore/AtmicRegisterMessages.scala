package se.kth.id2203.kvstore

import se.sics.kompics.sl._;

case class AR_Read_Request()                       extends KompicsEvent;
case class AR_Read_Response(value: Option[String]) extends KompicsEvent;
case class AR_Write_Request(value: String)         extends KompicsEvent;
case class AR_Write_Response()                     extends KompicsEvent

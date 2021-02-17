package se.kth.id2203.utils

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress

case class Leader(leader: NetAddress, group: Int) extends KompicsEvent

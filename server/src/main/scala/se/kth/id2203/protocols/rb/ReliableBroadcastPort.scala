package se.kth.id2203.protocols.rb

import se.sics.kompics.sl._;

class ReliableBroadcastPort extends Port {
  indication[RB_Deliver];
  request[RB_Broadcast];
}

package se.kth.id2203.protocols.beb

import se.sics.kompics.sl._;

class BestEffortBroadcastPort extends Port {
  indication[BEB_Deliver];
  request[BEB_Broadcast];
  request[BEB_Broadcast_Forward];
}

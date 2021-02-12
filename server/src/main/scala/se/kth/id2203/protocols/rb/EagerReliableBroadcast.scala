package se.kth.id2203.protocols.rb

import se.sics.kompics.sl._;
import se.kth.id2203.protocols.beb.{BEB_Broadcast, BEB_Deliver, BestEffortBroadcastPort}
import se.kth.id2203.networking.NetAddress

/** Eager implementation of reliable broadcast
  */
class EagerReliableBroadcast extends ComponentDefinition {
  val bebPort = requires[BestEffortBroadcastPort];
  val rbPort  = provides[ReliableBroadcastPort];

  val self = cfg.getValue[NetAddress]("id2203.project.address");

  val delivered = collection.mutable.Set[KompicsEvent]();

  rbPort uponEvent {
    case x @ RB_Broadcast(nodes, payload) => {
      trigger(BEB_Broadcast(nodes, x) -> bebPort);
    }
  }

  bebPort uponEvent {
    case BEB_Deliver(src, y @ RB_Broadcast(nodes, payload)) => {
      if (!delivered.contains(payload)) {
        delivered += payload;
        trigger(RB_Deliver(src, payload) -> rbPort);
        trigger(BEB_Broadcast(nodes, y)  -> bebPort)
      }
    }
  }
}

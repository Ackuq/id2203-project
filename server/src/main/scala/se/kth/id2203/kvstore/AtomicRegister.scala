package se.kth.id2203.kvstore

import se.sics.kompics.sl._;
import scala.collection.mutable.Map;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.perfect_link.PerfectLinkPort
import se.kth.id2203.protocols.beb.{BEB_Broadcast, BEB_Deliver, BestEffortBroadcastPort}
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Send}
import se.kth.id2203.utils.{GROUP};

class AtomicRegister extends ComponentDefinition {
  var (ts: Int, value: Option[String]) = (0, None);

  //***** Subscriptions
  val onar  = provides[AtomicRegisterPort];
  val pLink = requires[PerfectLinkPort];
  val beb   = requires[BestEffortBroadcastPort];

  var wts      = 0;
  var acks     = 0;
  var rid      = 0;
  var readList = Map.empty[NetAddress, (Int, Option[String])];

  // TODO: How do we populate replication group?
  var replicationGroup = Set.empty[NetAddress];

  var readVal: Option[String] = None;
  var reading                 = false;

  onar uponEvent {
    case AR_Read_Request() => {
      rid += 1;
      acks = 0;
      readList = Map.empty;
      reading = true;
      trigger(BEB_Broadcast(READ(rid)) -> beb)
    }
    case AR_Write_Request(value) => {
      rid += 1;
      wts += 1;
      acks = 0
      trigger(BEB_Broadcast(WRITE(rid, wts, Some(value))) -> beb)
    }
  }

  beb uponEvent {
    case BEB_Deliver(src, r: READ) => {
      trigger(PL_Send(src, VALUE(r.rid, ts, value)) -> pLink);
    }
    case BEB_Deliver(src, w: WRITE) => {
      if (w.ts > ts) {
        ts = w.ts;
        value = w.value;
      }
      trigger(PL_Deliver(src, ACK(w.rid)) -> pLink);
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, v: VALUE) => {
      if (v.rid == rid) {
        readList += (src -> (v.ts, v.value));
        if (readList.size > (replicationGroup.size / 2)) {
          // Get max ts
          val highest = readList.values.reduceLeft((x, y) => if (x._1 > y._1) x else y);
          val maxts   = highest._1;
          readVal = highest._2;
          readList = Map.empty;

          trigger(BEB_Broadcast(WRITE(rid, maxts, readVal)) -> beb);
        }
      }
    }
    case PL_Deliver(src, a: ACK) => {
      if (a.rid == rid) {
        acks += 1;
        if (acks > (replicationGroup.size) / 2) {
          acks = 0;
          reading match {
            case true => {
              reading = false;
              trigger(AR_Read_Response(readVal) -> onar)
            }
            case false => {
              trigger(AR_Write_Response() -> onar)
            }
          }
        }
      }
    }

    case PL_Deliver(self, GROUP(group)) => {
      replicationGroup = group;
    }
  }
}

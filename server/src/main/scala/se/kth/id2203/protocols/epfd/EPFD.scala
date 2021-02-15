package se.kth.id2203.protocols.epfd

import se.sics.kompics.sl._;
import se.sics.kompics.timer.{ScheduleTimeout, Timer}
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PL_Send, PerfectLinkPort}
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.utils.GROUP

class EPFD extends ComponentDefinition {

  // Subscriptions
  val timer = requires[Timer];
  val pLink = requires[PerfectLinkPort];
  val epfd  = provides[EPFDPort];

  // Config params
  val self     = cfg.getValue[NetAddress]("id2203.project.address");
  var topology = Set.empty[NetAddress];
  val delta    = cfg.getValue[Long]("epfd.delay");

  // Mutable state
  var period    = cfg.getValue[Long]("epfd.delay");
  var alive     = Set.empty[NetAddress];
  var suspected = Set.empty[NetAddress];
  var seqNum    = 0;

  def startTimer(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(period);
    scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }

  timer uponEvent {
    case CheckTimeout(_) => {
      if (!alive.intersect(suspected).isEmpty) {
        // Increase timeout
        period += delta;
      }
      seqNum += 1;

      for (p <- topology) {
        if (!alive.contains(p) && !suspected.contains(p)) {
          // Add p to suspects and trigger event
          suspected += p;
          trigger(Suspect(p) -> epfd);
        } else if (alive.contains(p) && suspected.contains(p)) {
          suspected -= p;
          trigger(Restore(p) -> epfd);
        }
        trigger(PL_Send(p, HeartbeatRequest(seqNum)) -> pLink)
      }
      alive = Set.empty[NetAddress];
      startTimer(period);
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, HeartbeatRequest(seq)) => {
      // Send heartbeat
      trigger(PL_Send(src, HeartbeatReply(seq)) -> pLink);
    }
    case PL_Deliver(src, HeartbeatReply(seq)) => {
      // Add src to alive if it responded
      if (seq == seqNum || suspected(src)) {
        alive += src;
      }
    }
    case PL_Deliver(self, GROUP(group)) => {
      topology = group;
      alive = Set(group.toList: _*);
    }
  }
}

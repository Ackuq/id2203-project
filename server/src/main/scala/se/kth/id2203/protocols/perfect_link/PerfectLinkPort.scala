package se.kth.id2203.protocols.perfect_link

import se.sics.kompics.sl._;

final class PerfectLinkPort extends Port {
  indication[PL_Deliver];
  request[PL_Send];
}

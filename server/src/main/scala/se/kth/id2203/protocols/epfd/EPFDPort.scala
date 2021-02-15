package se.kth.id2203.protocols.epfd;

import se.sics.kompics.sl._;

class EPFDPort extends Port {
  indication[Suspect];
  indication[Restore];
}

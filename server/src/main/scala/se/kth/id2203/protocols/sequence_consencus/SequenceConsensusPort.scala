package se.kth.id2203.protocols.sequence_consencus;

import se.sics.kompics.sl._;

class SequenceConsensusPort extends Port {
  request[SC_Propose];
  indication[SC_Decide];
}

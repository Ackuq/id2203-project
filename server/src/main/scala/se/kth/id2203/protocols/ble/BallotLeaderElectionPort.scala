package se.kth.id2203.protocols.ble

import se.sics.kompics.sl._;

class BallotLeaderElectionPort extends Port {
  indication[BLE_Leader];
}

package se.kth.id2203.simulation.scenarios;

import se.kth.id2203.simulation.simulators._;
import se.sics.kompics.sl._;
import se.sics.kompics.sl.simulator._;
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import scala.concurrent.duration._

object SimpleConsensusScenario {
  import Distributions._

  val serverBase = SimpleScenario.serverBase;

  // needed for the distributions, but needs to be initialised after setting the seed
  implicit val random = JSimulationScenario.getRandom();

  val startServerOp = Op { (self: Integer) =>
    val selfAddr = SimpleScenario.intToServerAddress(self)
    val conf = if (SimpleScenario.isBootstrap(self)) {
      // don't put this at the bootstrap server, or it will act as a bootstrap client
      Map("id2203.project.address" -> selfAddr)
    } else {
      Map("id2203.project.address"           -> selfAddr,
          "id2203.project.bootstrap-address" -> SimpleScenario.intToServerAddress(1)
      )
    };
    // Use our custom scenario server to keep track of the internal state
    StartNode(selfAddr, Init.none[ServerSimulator], conf);
  };

  def scenario(servers: Int): JSimulationScenario = {

    val networkSetup = raise(1, SimpleScenario.setUniformLatencyNetwork()).arrival(constant(0));
    val startCluster = raise(servers, startServerOp, 1.toN()).arrival(constant(1.second));
    val startClients = raise(1, SimpleScenario.startClientOp, 1.toN()).arrival(constant(1.second));

    networkSetup andThen
      0.seconds afterTermination startCluster andThen
      10.seconds afterTermination startClients andThen
      100.seconds afterTermination Terminate
  }
}

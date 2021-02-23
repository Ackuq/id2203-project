package se.kth.id2203.simulation.scenarios

import se.sics.kompics.sl.simulator._;
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import scala.concurrent.duration._

object SimpleCrashScenario {
  import Distributions._

  // needed for the distributions, but needs to be initialised after setting the seed
  implicit val random = JSimulationScenario.getRandom();

  val crashServerOp = Op { (self: Integer) =>
    val selfAddr = SimpleScenario.intToServerAddress(self);
    println(s"$selfAddr is gonna die")
    KillNode(selfAddr);
  }

  def scenario(servers: Int): JSimulationScenario = {
    val networkSetup = raise(1, SimpleScenario.setUniformLatencyNetwork()).arrival(constant(0));
    val startCluster = raise(servers, SimpleScenario.startServerOp, 1.toN()).arrival(constant(1.second));
    val crashServers = raise(1, crashServerOp, 2.toN()).arrival(constant(1.second));
    val startClients = raise(1, SimpleScenario.startClientOp, 1.toN()).arrival(constant(1.second));

    networkSetup andThen
      0.seconds afterTermination startCluster andThen
      10.seconds afterTermination crashServers andThen
      20.seconds afterTermination startClients andThen
      100.seconds afterTermination Terminate
  }
}

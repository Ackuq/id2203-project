package se.kth.id2203.simulation

import org.scalatest.flatspec.AnyFlatSpec;
import org.scalatest.matchers.must.Matchers
import java.net.{InetAddress, UnknownHostException};
import se.sics.kompics.network.Address
import se.sics.kompics.sl._;
import se.sics.kompics.sl.simulator._;
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import se.sics.kompics.simulator.run.LauncherComp
import se.sics.kompics.simulator.result.SimulationResultSingleton;
import se.sics.kompics.simulator.network.impl.NetworkModels
import scala.concurrent.duration._
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.simulation.sequence_consensus.{ParentComponent => ScenarioServer};

class SequenceConsensusTest extends AnyFlatSpec with Matchers {
  // Use 6 servers
  private val nServers  = 6;
  private val nMessages = 10;

  /* Validity:
      - If process p decides v then v is a sequence of proposed commands (without duplicates)
   */
  "Decided values" must "be non-duplicate proposed commands (Validity)" in {
    val seed = 123L;
    JSimulationScenario.setSeed(seed);
    val simpleBootScenario = SimpleSequenceConsensusScenario.scenario(nServers);
    val res                = SimulationResultSingleton.getInstance();
    SimulationResult += ("messages" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);

    var proposals = List.empty[String];
    var decided   = List.empty[String];

    for (s <- 1 to nServers) {
      val address     = SimpleSequenceConsensusScenario.serverBase + s;
      val numProposed = SimulationResult.get[Int](s"$address.numProposed").getOrElse(0);
      val numDecided  = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);

      for (i <- 0 to numProposed - 1) {
        proposals = proposals :+ SimulationResult.get[String](s"$address.proposed.$i").get;
      }

      for (i <- 0 to numDecided - 1) {
        decided = decided :+ SimulationResult.get[String](s"$address.decided.$i").get;
      }
    }
    for (decision <- decided) {
      proposals.contains(decision) must be(true)
    }
  }

  /* Uniform Agreement
      - If process p decides u and process q decides v then one is a prefix of the other
   */
  "Decided values" must "not diverge (Uniform Agreement)" in {
    val seed = 123L;
    JSimulationScenario.setSeed(seed);
    val simpleBootScenario = SimpleSequenceConsensusScenario.scenario(nServers);
    val res                = SimulationResultSingleton.getInstance();
    SimulationResult += ("messages" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);

    var decidedMap = Map.empty[String, List[String]];

    for (s <- 1 to nServers) {
      val address    = SimpleSequenceConsensusScenario.serverBase + s;
      val numDecided = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);
      var decided    = List.empty[String];
      for (i <- 0 to numDecided - 1) {
        decided = decided :+ SimulationResult.get[String](s"$address.decided.$i").get;
      }

      decidedMap = decidedMap + (address -> decided);
    }

    // Check that all servers has decided commands in same order
    for ((address, decided) <- decidedMap) {
      for ((otherAddress, otherDecided) <- decidedMap) {
        if (address != otherAddress) {
          for (i <- 0 to Math.min(decided.size, otherDecided.size) - 1) {
            decided(i) must be(otherDecided(i))
          }
        }
      }
    }
  }

  /** Termination (liveness)
    *  - If command C is proposed by a correct process then eventually every correct process decides a sequence containing C
    */
  "If a command is proposed then it" must "eventually be decided on in every correct process (Termination)" in {
    val seed = 123L;
    JSimulationScenario.setSeed(seed);
    val simpleBootScenario = SimpleSequenceConsensusScenario.scenario(nServers);
    val res                = SimulationResultSingleton.getInstance();
    SimulationResult += ("messages" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);

    var proposals  = List.empty[String];
    var decidedMap = Map.empty[String, List[String]];

    for (s <- 1 to nServers) {
      val address     = SimpleSequenceConsensusScenario.serverBase + s;
      val numProposed = SimulationResult.get[Int](s"$address.numProposed").getOrElse(0);
      val numDecided  = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);

      var decided = List.empty[String];

      for (i <- 0 to numProposed - 1) {
        proposals = proposals :+ SimulationResult.get[String](s"$address.proposed.$i").get;
      }

      for (i <- 0 to numDecided - 1) {
        decided = decided :+ SimulationResult.get[String](s"$address.decided.$i").get;
      }

      decidedMap = decidedMap + (address -> decided);
    }

    for (proposal <- proposals) {
      for ((_, decided) <- decidedMap) {
        decided.contains(proposal) must be(true);
      }
    }
  }
}

object SimpleSequenceConsensusScenario {

  val serverBase = "192.193.0.";

  import Distributions._
  // needed for the distributions, but needs to be initialised after setting the seed
  implicit val random = JSimulationScenario.getRandom();

  private def intToServerAddress(i: Int): Address = {
    try {
      NetAddress(InetAddress.getByName(serverBase + i), 45678);
    } catch {
      case ex: UnknownHostException => throw new RuntimeException(ex);
    }
  }
  private def intToClientAddress(i: Int): Address = {
    try {
      NetAddress(InetAddress.getByName("192.193.1." + i), 45678);
    } catch {
      case ex: UnknownHostException => throw new RuntimeException(ex);
    }
  }

  private def isBootstrap(self: Int): Boolean = self == 1;

  val setUniformLatencyNetwork = () => Op.apply((_: Unit) => ChangeNetwork(NetworkModels.withConstantDelay(2)));

  val startServerOp = Op { (self: Integer) =>
    val selfAddr = intToServerAddress(self)
    val conf = if (isBootstrap(self)) {
      // don't put this at the bootstrap server, or it will act as a bootstrap client
      Map("id2203.project.address" -> selfAddr)
    } else {
      Map("id2203.project.address" -> selfAddr, "id2203.project.bootstrap-address" -> intToServerAddress(1))
    };
    // Use our custom scenario server to keep track of the internal state
    StartNode(selfAddr, Init.none[ScenarioServer], conf);
  };

  val startClientOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf     = Map("id2203.project.address" -> selfAddr, "id2203.project.bootstrap-address" -> intToServerAddress(1));
    StartNode(selfAddr, Init.none[ScenarioClient], conf);
  };

  def scenario(servers: Int): JSimulationScenario = {

    val networkSetup = raise(1, setUniformLatencyNetwork()).arrival(constant(0));
    val startCluster = raise(servers, startServerOp, 1.toN()).arrival(constant(1.second));
    val startClients = raise(1, startClientOp, 1.toN()).arrival(constant(1.second));

    networkSetup andThen
      0.seconds afterTermination startCluster andThen
      10.seconds afterTermination startClients andThen
      100.seconds afterTermination Terminate
  }
}

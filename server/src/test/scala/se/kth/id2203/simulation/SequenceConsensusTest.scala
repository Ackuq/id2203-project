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
import scala.collection.mutable

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

    var proposals  = List.empty[String];
    var allDecided = List.empty[String];

    for (s <- 1 to nServers) {
      val address     = SimpleSequenceConsensusScenario.serverBase + s;
      val numProposed = SimulationResult.get[Int](s"$address.numProposed").getOrElse(0);
      val numDecided  = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);
      var decided     = List.empty[String];

      for (i <- 0 to numProposed - 1) {
        proposals = proposals :+ SimulationResult.get[String](s"$address.proposed.$i").get;
      }

      for (i <- 0 to numDecided - 1) {
        decided = decided :+ SimulationResult.get[String](s"$address.decided.$i").get;
      }
      // Check for duplicates
      decided.size must be(decided.distinct.size)
    }

    // Check that all decisions are proposals
    for (decision <- allDecided) {
      proposals.contains(decision) must be(true);
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

    var decidedMap = mutable.Map.empty[Int, Map[String, List[String]]];

    for (s <- 1 to nServers) {
      val address    = SimpleSequenceConsensusScenario.serverBase + s;
      val partition  = SimulationResult.get[Int](s"$address.partition").get
      val numDecided = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);
      var decided    = List.empty[String];

      if (!decidedMap.contains(partition)) {
        decidedMap(partition) = Map.empty;
      }

      for (i <- 0 to numDecided - 1) {
        decided = decided :+ SimulationResult.get[String](s"$address.decided.$i").get;
      }

      decidedMap(partition) = decidedMap(partition) + (address -> decided);
    }

    // Check that all servers has decided commands in same order
    for (partition <- decidedMap.keys) {
      // Check against all different nodes within same partition
      for ((address, decided) <- decidedMap(partition)) {
        for ((otherAddress, otherDecided) <- decidedMap(partition)) {
          // Skip checking self
          if (address != otherAddress) {
            // They should both have the same length, but we'll use Math.min in case of prefixes
            for (i <- 0 to Math.min(decided.size, otherDecided.size) - 1) {
              decided(i) must be(otherDecided(i))
            }
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

    var allProposals = List.empty[String];
    var allDecision  = List.empty[String];

    for (s <- 1 to nServers) {
      val address     = SimpleSequenceConsensusScenario.serverBase + s;
      val numProposed = SimulationResult.get[Int](s"$address.numProposed").getOrElse(0);
      val numDecided  = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);

      var decided = List.empty[String];

      for (i <- 0 to numProposed - 1) {
        val proposal = SimulationResult.get[String](s"$address.proposed.$i").get;
        allProposals = allProposals :+ proposal;
      }

      for (i <- 0 to numDecided - 1) {
        val decision = SimulationResult.get[String](s"$address.decided.$i").get;
        allDecision = allDecision :+ decision;
      }
    }

    // All proposals should be decided by some node
    for (proposal <- allProposals) {
      allDecision.contains(proposal) must be(true)
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

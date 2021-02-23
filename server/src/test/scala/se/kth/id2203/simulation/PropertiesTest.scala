package se.kth.id2203.simulation

import se.kth.id2203.simulation.scenarios.SimpleConsensusScenario;
import org.scalatest.flatspec.AnyFlatSpec;
import org.scalatest.matchers.should.Matchers
import se.sics.kompics.sl.simulator._;
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import se.sics.kompics.simulator.run.LauncherComp
import se.sics.kompics.simulator.result.SimulationResultSingleton;
import scala.collection.mutable
import org.scalatest.BeforeAndAfterAll
import org.scalatest.DoNotDiscover

/** Test suite that tests that verifies the properties of the KV-store
  */
@DoNotDiscover
class PropertiesTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  // Use 6 servers
  private val nServers  = 6;
  private val nMessages = 10;

  override def beforeAll(): Unit = {
    val seed = 123L;
    JSimulationScenario.setSeed(seed);
    val simpleBootScenario = SimpleConsensusScenario.scenario(nServers);
    val _                  = SimulationResultSingleton.getInstance();
    SimulationResult += ("messages" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);
  }

  /* Linearizability/Atomicity
      - Only allow executions whose results appear as if there is a single system image and “global time” is obeyed
   */
  "Executions" should "be linearizabile" in {
    // partition_index -> (address -> List<op>)
    val decidedMap = mutable.Map.empty[Int, Map[String, List[String]]];
    // partition_index -> List<op>
    val proposedMap = mutable.Map.empty[Int, List[String]]

    for (s <- 1 to nServers) {
      val address     = SimpleConsensusScenario.serverBase + s;
      val partition   = SimulationResult.get[Int](s"$address.partition").get;
      val numProposed = SimulationResult.get[Int](s"$address.numProposed").getOrElse(0);
      val numDecided  = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);
      var decided     = List.empty[String];

      if (!decidedMap.contains(partition)) {
        decidedMap(partition) = Map.empty;
      }

      if (!proposedMap.contains(partition)) {
        proposedMap(partition) = List.empty;
      }

      for (i <- 0 to numProposed - 1) {
        val proposed = SimulationResult.get[String](s"$address.proposed.$i").get;
        proposedMap(partition) = proposedMap(partition) :+ proposed;
      }

      for (i <- 0 to numDecided - 1) {
        val decision = SimulationResult.get[String](s"$address.decided.$i").get;
        decided = decided :+ decision;
      }

      decidedMap(partition) += (address -> decided)
    }

    for ((partition, proposed) <- proposedMap) {
      for (decided <- decidedMap(partition).values) {
        // We should not have decided unproposed values
        decided.size <= proposed.size should be(true);
        for (i <- 0 to decided.size - 1) {
          // They should be decided in same order as proposed for all nodes in partition
          decided(i) should be(proposed(i))
        }
      }
    }
  }

  /* Validity:
      - If process p decides v then v is a sequence of proposed commands (without duplicates)
   */
  "Decided values" should "be non-duplicate proposed commands (Validity)" in {

    var proposals  = List.empty[String];
    var allDecided = List.empty[String];

    for (s <- 1 to nServers) {
      val address     = SimpleConsensusScenario.serverBase + s;
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
      decided.size should be(decided.distinct.size)
      allDecided = allDecided ++ decided;
    }

    allDecided.size should not be 0;
    proposals.size should not be 0;

    // Check that all decisions are proposals
    for (decision <- allDecided) {
      proposals.contains(decision) should be(true);
    }
  }

  /* Uniform Agreement
      - If process p decides u and process q decides v then one is a prefix of the other
   */
  "Decided values" should "not diverge (Uniform Agreement)" in {
    val decidedMap = mutable.Map.empty[Int, Map[String, List[String]]];

    for (s <- 1 to nServers) {
      val address    = SimpleConsensusScenario.serverBase + s;
      val partition  = SimulationResult.get[Int](s"$address.partition").get;
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

    decidedMap.size should not be 0

    // Check that all servers has decided commands in same order
    for (partition <- decidedMap.keys) {
      // Check against all different nodes within same partition
      for ((address, decided) <- decidedMap(partition)) {
        for ((otherAddress, otherDecided) <- decidedMap(partition)) {
          // Skip checking self
          if (address != otherAddress) {
            // They should both have the same length, but we'll use Math.min in case of prefixes
            for (i <- 0 to Math.min(decided.size, otherDecided.size) - 1) {
              decided(i) should be(otherDecided(i))
            }
          }
        }
      }
    }
  }

  /** Termination (liveness)
    *  - If command C is proposed by a correct process then eventually every correct process decides a sequence containing C
    */
  "If a command is proposed then it" should "eventually be decided on in every correct process (Termination)" in {
    var allProposals = List.empty[String];
    var allDecision  = List.empty[String];

    for (s <- 1 to nServers) {
      val address     = SimpleConsensusScenario.serverBase + s;
      val numProposed = SimulationResult.get[Int](s"$address.numProposed").getOrElse(0);
      val numDecided  = SimulationResult.get[Int](s"$address.numDecided").getOrElse(0);

      for (i <- 0 to numProposed - 1) {
        val proposal = SimulationResult.get[String](s"$address.proposed.$i").get;
        allProposals = allProposals :+ proposal;
      }

      for (i <- 0 to numDecided - 1) {
        val decision = SimulationResult.get[String](s"$address.decided.$i").get;
        allDecision = allDecision :+ decision;
      }
    }

    allProposals.size should not be 0;
    allDecision.size should not be 0;

    // All proposals should be decided by some node
    for (proposal <- allProposals) {
      allDecision.contains(proposal) should be(true)
    }
  }
}

package se.kth.id2203.simulation

import org.scalatest.flatspec.AnyFlatSpec;
import org.scalatest.matchers.should.Matchers
import se.sics.kompics.sl.simulator._;
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import se.sics.kompics.simulator.run.LauncherComp
import se.sics.kompics.simulator.result.SimulationResultSingleton;
import scala.collection.mutable
import org.scalatest.BeforeAndAfterAll

class OperationsAndProperties extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  // Use 6 servers
  private val nServers  = 6;
  private val nMessages = 10;

  override def beforeAll(): Unit = {
    val seed = 123L;
    JSimulationScenario.setSeed(seed);
    val simpleBootScenario = SimpleScenario.scenario(nServers);
    val _                  = SimulationResultSingleton.getInstance();
    SimulationResult += ("messages" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);
  }

  /** Test that all operations got expected response
    */
  "Operations" should "be implemented" in {
    for (i <- 0 to nMessages) {
      // PUTs
      SimulationResult.get[String](s"key$i.put") should be(Some(s"value$i"));
    }
    for (i <- 0 to nMessages) {
      // GETs
      SimulationResult.get[String](s"key$i.get") should be(Some(s"value$i"));
    }
    for (i <- 0 to nMessages) {
      // Failing CASs, should remain same
      SimulationResult.get[String](s"key$i.cas_1") should be(Some(s"value$i"));
    }
    for (i <- 0 to nMessages) {
      // Correct CAS, value should be changed to newValue$i
      SimulationResult.get[String](s"key$i.cas_2") should be(Some(s"newValue$i"));
    }
  }

  /* Validity:
      - If process p decides v then v is a sequence of proposed commands (without duplicates)
   */
  "Decided values" should "be non-duplicate proposed commands (Validity)" in {

    var proposals  = List.empty[String];
    var allDecided = List.empty[String];

    for (s <- 1 to nServers) {
      val address     = SimpleScenario.serverBase + s;
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
      val address    = SimpleScenario.serverBase + s;
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
      val address     = SimpleScenario.serverBase + s;
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

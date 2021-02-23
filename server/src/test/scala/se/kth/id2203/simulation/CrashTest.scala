package se.kth.id2203.simulation

import se.kth.id2203.simulation.scenarios.SimpleCrashScenario;
import org.scalatest.flatspec.AnyFlatSpec;
import org.scalatest.matchers.should.Matchers
import se.sics.kompics.sl.simulator._;
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import se.sics.kompics.simulator.run.LauncherComp
import se.sics.kompics.simulator.result.SimulationResultSingleton;
import org.scalatest.BeforeAndAfter;
import org.scalatest.DoNotDiscover

/** Test suite that tests if the system still functions after node failure
  */
@DoNotDiscover
class CrashTest extends AnyFlatSpec with Matchers with BeforeAndAfter {

  private val nServers = 10;
  val nMessages        = 10;

  before {
    val seed = 123L;
    JSimulationScenario.setSeed(seed);
    val simpleBootScenario = SimpleCrashScenario.scenario(nServers);
    val _                  = SimulationResultSingleton.getInstance();
    SimulationResult += ("messages" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);
  }

  /** Test that all operations got expected response
    */
  "System" should "work even if nodes die" in {
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

}

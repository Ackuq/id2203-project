package se.kth.id2203.simulation.sequence_consensus;

import se.sics.kompics.sl._;
import se.sics.kompics.sl.simulator.SimulationResult;
import se.sics.kompics.network.Network;

import se.kth.id2203.overlay.RouteMsg;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.kvstore._;
import java.util.UUID;
import se.sics.kompics.timer.Timer;
import scala.collection.mutable;
import se.kth.id2203.networking.NetMessage;
import se.kth.id2203.protocols.sequence_consencus.{SC_Decide, SC_Propose, SequenceConsensusPort};

class ScenarioServer extends ComponentDefinition {
  //******* Ports ******
  private val seqCons = requires[SequenceConsensusPort]
  //******* Fields ******
  private val self   = cfg.getValue[NetAddress]("id2203.project.address");
  private val server = cfg.getValue[NetAddress]("id2203.project.bootstrap-address");

  private var numDecided  = 0;
  private var numProposed = 0;

  def decided(command: String): Unit = {
    SimulationResult += (s"$self.decided.$numDecided" -> command);
    numDecided += 1;
    SimulationResult += (s"$self.numDecided" -> numDecided)
  }

  def proposed(command: String): Unit = {
    SimulationResult += (s"$self.proposed.$numProposed" -> command);
    numProposed += 1;
    SimulationResult += (s"$self.numProposed" -> numProposed);
  }

  seqCons uponEvent {
    case SC_Decide(DecidedOperation(cmd, _)) => {
      decided(cmd.toString());
    }
    case SC_Propose(cmd) => {
      proposed(cmd.toString());
    }
  }
}

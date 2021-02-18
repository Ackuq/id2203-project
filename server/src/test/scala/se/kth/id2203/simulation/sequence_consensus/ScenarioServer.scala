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

class ScenarioServer(init: Init[ScenarioServer]) extends ComponentDefinition {
  //******* Ports ******
  private val seqCons        = requires[SequenceConsensusPort]
  private val seqConsForward = provides[SequenceConsensusPort];
  //******* Fields ******
  private val partition = init match {
    case Init(partition: Int) => partition
    case _                    => 0
  }
  private val self   = cfg.getValue[NetAddress]("id2203.project.address").getIp().getHostAddress();
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

  //******* Handlers ******

  ctrl uponEvent {
    case _: Start => {
      SimulationResult += (s"$self.partition" -> partition)
    }
  }

  seqCons uponEvent {
    case SC_Decide(DecidedOperation(cmd, _)) => {
      decided(cmd.toString());
    }
  }

  seqConsForward uponEvent {
    case x @ SC_Propose(cmd) => {
      proposed(cmd.toString());
      trigger(x -> seqCons)
    }
  }
}

package se.kth.id2203.kvstore

import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress
import se.kth.id2203.protocols.sequence_consencus.RSM_Command
import se.kth.id2203.protocols.sequence_consencus.Role

trait ProposedOperationTrait extends RSM_Command {
  def src: NetAddress;
  def command: Op;
}

trait DecidedOperationTrait extends RSM_Command {
  def proposed: RSM_Command;
  def role: Role.Value;
}

case class ProposedOperation(src: NetAddress, command: Op)           extends ProposedOperationTrait;
case class DecidedOperation(proposed: RSM_Command, role: Role.Value) extends DecidedOperationTrait;

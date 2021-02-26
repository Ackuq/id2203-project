/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.kvstore;

import se.kth.id2203.networking._;
import se.sics.kompics.sl._;
import se.sics.kompics.network.Network;
import scala.collection.mutable.HashMap
import se.kth.id2203.protocols.perfect_link.{PL_Deliver, PerfectLinkPort}
import se.kth.id2203.protocols.sequence_consencus.{SC_Decide, SC_Propose, SequenceConsensusPort}
import se.kth.id2203.protocols.sequence_consencus.Role

/** Key-value store */
class KVService extends ComponentDefinition {

  type Key   = String;
  type Value = String;

  //******* Ports ******
  private val net = requires[Network];
  //******* Custom ports ******
  private val pLink   = requires[PerfectLinkPort];
  private val seqCons = requires[SequenceConsensusPort];
  //******* Fields ******
  private val self  = cfg.getValue[NetAddress]("id2203.project.address");
  private val store = HashMap[Key, Value]()
  //******* Handlers ******
  pLink uponEvent {
    case PL_Deliver(src, op: Op) => {
      //log.info(s"KV-Store at $self got operation $op");
      trigger(SC_Propose(ProposedOperation(src, op)) -> seqCons)
    }
  }

  seqCons uponEvent {
    case SC_Decide(DecidedOperation(ProposedOperation(src, op), role: Role.Value)) => {
      if (role == Role.LEADER) {
        log.info(s"[$self] committing $op");
      }
      op match {
        case PUT(key, value, _) => {
          store.synchronized(store.put(key, value))

          if (role == Role.LEADER) {
            trigger(
              NetMessage(self, src, op.response(value, OpCode.Ok)) -> net
            )
          }
        }
        case GET(key, _) =>
          if (role == Role.LEADER) {
            store.synchronized(store.get(key)) match {
              case Some(value) => trigger(NetMessage(self, src, op.response(value, OpCode.Ok)) -> net)
              case None =>
                trigger(NetMessage(self, src, op.response(OpCode.NotFound)) -> net)
            }
          }
        case CAS(key, oldValue, newValue, _) => {
          store.synchronized {
            store.get(key) match {
              case Some(value) => {
                if (value == oldValue) {
                  store.put(key, newValue)
                  if (role == Role.LEADER) {
                    trigger(NetMessage(self, src, op.response(newValue, OpCode.Ok)) -> net)

                  }
                } else if (role == Role.LEADER) {
                  trigger(NetMessage(self, src, op.response(value, OpCode.NotModified)) -> net)
                }
              }
              case None => {
                if (role == Role.LEADER) {
                  trigger(NetMessage(self, src, op.response(OpCode.NotFound)) -> net)
                }
              }
            }
          }
        }
        case _ => {
          if (role == Role.LEADER) {
            trigger(NetMessage(self, src, op.response(OpCode.NotImplemented)) -> net)
          }
        }
      }
    }
  }
}

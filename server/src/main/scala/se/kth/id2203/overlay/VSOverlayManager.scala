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
package se.kth.id2203.overlay;

import se.kth.id2203.bootstrapping._;
import se.kth.id2203.networking._;
import se.sics.kompics.sl._;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import util.Random;
import se.kth.id2203.utils.{GROUP};
import se.kth.id2203.protocols.rb.{RB_Broadcast, ReliableBroadcastPort};
import se.kth.id2203.protocols.perfect_link.{PL_Forward, PerfectLinkPort};
import se.kth.id2203.protocols.perfect_link.PL_Send;

/** The V(ery)S(imple)OverlayManager.
  * <p>
  * Keeps all nodes in a single partition in one replication group.
  * <p>
  * Note: This implementation does not fulfill the project task. You have to
  * support multiple partitions!
  * <p>
  * @author Lars Kroll <lkroll@kth.se>
  */
class VSOverlayManager extends ComponentDefinition {

  //******* Ports ******
  val route = provides(Routing);
  val boot  = requires(Bootstrapping);
  val net   = requires[Network];
  val timer = requires[Timer];
  //******* Custom Ports ******
  val pLink = requires[PerfectLinkPort];
  val rb    = requires[ReliableBroadcastPort];
  //******* Fields ******
  val self                             = cfg.getValue[NetAddress]("id2203.project.address");
  private var lut: Option[LookupTable] = None;
  //******* Handlers ******
  boot uponEvent {
    case GetInitialAssignments(nodes) => {
      log.info("Generating LookupTable...");
      val lut = LookupTable.generate(nodes,
                                     cfg.getValue[Int]("id2203.project.replicationDegree"),
                                     cfg.getValue[Int]("id2203.project.keyRange")
      );
      logger.debug(s"Generated assignments:\n$lut");
      trigger(new InitialAssignments(lut) -> boot);
    }
    case Booted(assignment: LookupTable) => {
      log.info("Got NodeAssignment, overlay ready.");
      lut = Some(assignment);

      // Our current group
      val myGroup = lut.get.getGroup(self);

      // Send this to every component that might need
      trigger(PL_Send(self, GROUP(myGroup)) -> pLink);
    }
  }

  net uponEvent {
    case NetMessage(header, RouteMsg(key, msg)) => {
      val nodes = lut.get.lookup(key).toSet;
      assert(!nodes.isEmpty);

      if (nodes.contains(self)) {
        /* Broadcast to group
         * TODO: Proper atomic register broadcast
         */
        // trigger(RB_Broadcast(nodes, msg) -> rb)
        for (node <- nodes) {
          trigger(PL_Forward(header.src, node, msg) -> pLink);
        }
      } else {
        /* Forward to some member in the designated replication group
          The receiving node will take care of the broadcast */
        val i      = Random.nextInt(nodes.size);
        val target = nodes.drop(i).head;
        log.info(s"Forwarding message for key $key to $target");
        trigger(NetMessage(header.src, target, msg) -> net);
      }

    }
    case NetMessage(header, msg: Connect) => {
      lut match {
        case Some(l) => {
          log.debug("Accepting connection request from ${header.src}");
          val size = l.getNodes().size;
          trigger(NetMessage(self, header.src, msg.ack(size)) -> net);
        }
        case None => log.info("Rejecting connection request from ${header.src}, as system is not ready, yet.");
      }
    }
  }

  route uponEvent {
    case RouteMsg(key, msg) => {
      val nodes = lut.get.lookup(key);
      assert(!nodes.isEmpty);
      val i      = Random.nextInt(nodes.size);
      val target = nodes.drop(i).head;
      log.info(s"Routing message for key $key to $target");
      trigger(NetMessage(self, target, msg) -> net);
    }
  }
}

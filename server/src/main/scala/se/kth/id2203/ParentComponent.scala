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
package se.kth.id2203;

import se.kth.id2203.bootstrapping._;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay._
import se.sics.kompics.sl._
import se.sics.kompics.Init;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/** Custom Protocol */
import se.kth.id2203.protocols.perfect_link.{PerfectLink, PerfectLinkPort}
import se.sics.kompics.Component;

class ParentComponent extends ComponentDefinition {

  //******* Ports ******
  val net: PositivePort[Network] = requires[Network];
  val timer: PositivePort[Timer] = requires[Timer];
  //******* Custom components ******
  val pLink: Component = create(classOf[PerfectLink], Init.NONE);
  //******* Children ******
  val overlay: Component = create(classOf[VSOverlayManager], Init.NONE);
  val boot: Component = cfg.readValue[NetAddress]("id2203.project.bootstrap-address") match {
    case Some(_) => create(classOf[BootstrapClient], Init.NONE); // start in client mode
    case None    => create(classOf[BootstrapServer], Init.NONE); // start in server mode
  }

  val consensusWrapper: Component = create(classOf[ConsensusWrapper], Init.NONE);

  {
    // Perfect Link
    connect[Network](net -> pLink);
    // Bootstrap
    connect[Timer](timer -> boot);
    connect[Network](net -> boot);
    // Overlay
    connect(Bootstrapping)(boot    -> overlay);
    connect[Network](net           -> overlay);
    connect[PerfectLinkPort](pLink -> overlay);
    // Replica wrapper
    connect[PerfectLinkPort](pLink -> consensusWrapper);
    connect[Network](net           -> consensusWrapper);
    connect(Bootstrapping)(boot    -> consensusWrapper);
    connect[Timer](timer           -> consensusWrapper);
  }
}

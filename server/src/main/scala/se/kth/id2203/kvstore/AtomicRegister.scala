package se.kth.id2203.kvstore

import se.sics.kompics.sl._;
import scala.collection.mutable.Map;
import se.kth.id2203.networking.NetAddress

class AtomicRegister extends ComponentDefinition {
  var (ts, value) = (0, None);

  val wts                            = 0;
  val acks                           = 0;
  val rid                            = 0;
  val readList: Map[NetAddress, Int] = Map.empty;

  val readVal: Option[String] = None;
  val reading                 = false;

}

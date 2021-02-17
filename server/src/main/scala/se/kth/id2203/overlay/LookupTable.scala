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

import com.larskroll.common.collections._;
import java.util.Collection;
import se.kth.id2203.bootstrapping.NodeAssignment;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.sl.Config;

/** Lookup table to be able to lookup the nodes that should replicate
  * @param replicationDegree In how many nodes a value should be replicated
  * @param keyRange How many keys we can have per partition
  */
@SerialVersionUID(6322485231428233902L)
class LookupTable(val replicationDegree: Int, val keyRange: Int) extends NodeAssignment with Serializable {

  val partitions = TreeSetMultiMap.empty[Int, NetAddress];

  /** Lookup the nodes we should forward message to
    * @param key Key associated with message
    * @return An iterable of nodes (addresses)
    */
  def lookup(key: String): Iterable[NetAddress] = {
    return partitions(getPartitionIndex(key));
  }

  /** Get the partition index of a key
    * @param key The key we are interested in
    * @return The index of the concerned group
    */
  def getPartitionIndex(key: String): Int = {
    val keyHash = key.hashCode() % (keyRange * partitions.size);
    return (keyHash.toFloat / (keyRange + 1)).floor.toInt;
  }

  /** Get the partition for an address
    * @param address The address we are interested in
    * @return A tuple (index, partition), where the partition is an iterable of nodes
    */
  def getPartition(address: NetAddress): (Int, Iterable[NetAddress]) = {
    for ((key, partition) <- partitions) {
      if (partition.exists(a => a.sameHostAs(address))) {
        return (key, partition);
      }
    }
    throw new IllegalArgumentException("Woops, you're not in a group, something went wrong...");
  }

  def getNodes(): Set[NetAddress] = {
    partitions.foldLeft(Set.empty[NetAddress]) { case (acc, kv) =>
      acc ++ kv._2
    }
  }

  override def toString(): String = {
    val sb = new StringBuilder();
    sb.append("LookupTable(\n");
    sb.append(partitions.mkString(","));
    sb.append(")");
    return sb.toString();
  }

}

object LookupTable {

  /** Generate the partitions responsible for the replication of data
    * @param nodes Nodes in cluster.
    * @param replicationDegree In how many nodes a value should be replicated
    * @param keyRange How many keys we can have per partition
    * @return LookupTable
    */
  def generate(nodes: Set[NetAddress], replicationDegree: Int, keyRange: Int): LookupTable = {
    val lut           = new LookupTable(replicationDegree, keyRange);
    val partitionSize = lut.replicationDegree + 1

    var partition = 0

    /* Divide into partitions and add the partitions to the table */
    nodes
      .dropRight(nodes.size % replicationDegree)
      .sliding(partitionSize, partitionSize)
      .foreach(group => {
        lut.partitions ++= (partition -> group);
        partition += 1;
      })

    println(s"Created ${lut.partitions.size} partitions");
    lut
  }
}

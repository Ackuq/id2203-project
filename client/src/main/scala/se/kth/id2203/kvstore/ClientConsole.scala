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
package se.kth.id2203.kvstore

import com.larskroll.common.repl._
import com.typesafe.scalalogging.StrictLogging;
import org.apache.log4j.Layout
import util.log4j.ColoredPatternLayout;
import fastparse._, NoWhitespace._
import concurrent.{Await, Future => ScalaFuture}
import concurrent.duration._
import io.netty.util.concurrent.Future

object ClientConsole {
  def lowercase[_: P] = P(CharIn("a-z"))
  def uppercase[_: P] = P(CharIn("A-Z"))
  def digit[_: P]     = P(CharIn("0-9"))
  def simpleStr[_: P] = P(lowercase | uppercase | digit)
  val colouredLayout  = new ColoredPatternLayout("%d{[HH:mm:ss,SSS]} %-5p {%c{1}} %m%n");
}

class ClientConsole(val service: ClientService) extends CommandConsole with ParsedCommands with StrictLogging {
  import ClientConsole._;

  override def layout: Layout      = colouredLayout;
  override def onInterrupt(): Unit = exit();

  private def onSent(fr: ScalaFuture[OpResponse]): Unit = {
    out.println("Operation sent! Awaiting response...");
    try {
      val r = Await.result(fr, 5.seconds);
      r.result match {
        case Some(value) =>
          out.println(s"Operation complete! Server responded with value $value and code ${r.status}");
        case None =>
          out.println("Operation complete! Response was: " + r.status);
      }
    } catch {
      case e: Throwable => logger.error("Error during operation.", e);
    }
  }

  val putParser = new ParsingObject[(String, String)] {
    override def parseOperation[_: P]: P[(String, String)] = P(("PUT" | "put") ~ " " ~ simpleStr.! ~ " " ~ simpleStr.!);
  }

  val putCommand =
    parsed(putParser, usage = "PUT <key> <value>", descr = "Executes a PUT for <key> with value <value>.") {
      case (key, value) =>
        println(s"PUT with key $key and value $value");

        val fr = service.put(key, value);
        out.print("PUT sent! Awaiting response...");
        onSent(fr);
    }

  val getParser = new ParsingObject[String] {
    override def parseOperation[_: P]: P[String] = P(("GET" | "get") ~ " " ~ simpleStr.!);
  }

  val getCommand =
    parsed(getParser, usage = "GET <key>", descr = "Executes a GET for <key>.") { case key =>
      println(s"GET with key $key");

      val fr = service.get(key);
      out.println(s"GET sent! Awaiting response...");
      onSent(fr);
    }
}

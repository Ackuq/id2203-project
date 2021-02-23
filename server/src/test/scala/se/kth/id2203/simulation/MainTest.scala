package se.kth.id2203.simulation

import org.scalatest.Suites

/* These could possibly interact with each other, so run them sequentially */
class MainTest extends Suites(new OperationsTest, new PropertiesTest, new CrashTest)

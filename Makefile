compile:
	sbt compile

assemble:
	sbt assembly

run-client:
	java -jar client/target/scala-2.13/client.jar -p 45678 -s localhost:45678

run-server:
	java -jar server/target/scala-2.13/server.jar -p 56787

client: compile assemble run-client

server: compile assemble run-server

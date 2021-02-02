DIR=$(shell pwd)

all: compile assemble

compile:
	sbt compile

assemble:
	sbt assembly

run-bootstrap-server:
	osascript -e 'tell app "Terminal" to do script "cd $(DIR) && java -jar server/target/scala-2.13/server.jar -p 45678"'

CLUSTER_SIZE?=3

run-server:
	n=1; \
	while [ $$n -le $(CLUSTER_SIZE) ]; do \
		port=`expr 45678 + $$n`; \
		osascript -e 'tell app "Terminal" to do script "cd $(DIR) && java -jar server/target/scala-2.13/server.jar -p '$$port' -s localhost:45678"'; \
		n=`expr $$n + 1`; \
	done

run-client:
	java -jar client/target/scala-2.13/client.jar -p 56787 -s localhost:45678


client: compile assemble run-client

server: compile assemble run-server

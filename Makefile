DIR=$(shell pwd)

all: compile assemble

compile: compile-common compile-client compile-server

assemble: assemble-client assemble-server

### Client ###

client: compile-client assemble-client run-client

run-client:
	java -jar client/target/scala-2.13/client.jar -p 56787 -s localhost:45678

assemble-client:
	sbt client/assembly

compile-client:
	sbt client/compile

### Server ###

CLUSTER_SIZE?=6

server: compile-server assemble-server run-server

run-bootstrap-server:
	osascript -e 'tell app "Terminal" to do script "cd $(DIR) && java -jar server/target/scala-2.13/server.jar -p 45678"'

run-servers:
	n=1; \
	while [ $$n -le `expr $(CLUSTER_SIZE) - 1` ]; do \
		port=`expr 45678 + $$n`; \
		osascript -e 'tell app "Terminal" to do script "cd $(DIR) && java -jar server/target/scala-2.13/server.jar -p '$$port' -s localhost:45678"'; \
		n=`expr $$n + 1`; \
	done

run-server: run-bootstrap-server run-servers

assemble-server:
	sbt server/assembly

compile-server:
	sbt server/compile

### Common ###

compile-common:
	sbt common/compile

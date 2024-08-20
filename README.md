# servicebus-concurrentcalls-cores

## Prerequisite

1.	[Docker](https://docs.docker.com/desktop/install/windows-install/).
2.	[IntelliJ]( https://www.jetbrains.com/idea/download/?section=windows) with [Docker plugin]( https://www.jetbrains.com/help/idea/docker.html#install_docker). The plugin is available by default in IntelliJ IDEA Ultimate.
3.    Java 11 (any higher version should work, but 11 is what is used to develop the sample)
```
      $ mvn --version
      Apache Maven 3.8.6
      Maven home: <path>/apache-maven-3.8.6
      Java version: 11.0.22, vendor: Azul Systems, Inc., runtime: <path>/Zulu/zulu-11
```

4. settings.env

Create the file `settings.env` in the root directory (i.e. `./servicebus-concurrentcalls-cores` ) with the following content:

```
AZURE_SERVICEBUS_CONNECTION_STRING=<connection-string>
AZURE_SERVICEBUS_QUEUE_NAME=queue0
```

Replace `<connection-string>` with the connection string of your Azure Service Bus namespace.

## Running the docker services

1.	Clone the repo.
2.	Open the folder `servicebus-concurrentcalls-cores` in IntelliJ.
3.	From the IntelliJ terminal switch to `messaging-client-scenarios` and package the Java App
      > C:\code1\servicebus-concurrentcalls-cores> cd messaging-client-scenarios

      > C:\code1\servicebus-concurrentcalls-cores\messaging-client-scenarios> mvn clean package spring-boot:repackage
4. Right-click on the `docker-compose.yml` and select `Run 'docker-compose.yml: …'`
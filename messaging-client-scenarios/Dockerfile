ARG REGISTRY="azsdkengsys.azurecr.io"

FROM ${REGISTRY}/java/jdk-mariner-mvn:jdk11-latest as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM mcr.microsoft.com/openjdk/jdk:11-mariner
RUN yum -y update && \
    yum -y install wget && \
    yum install -y tar.x86_64 && \
    yum clean all
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
 ## expose the port for remote debugging the Java app locally.
EXPOSE 7777 7777

ENTRYPOINT java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:7777 org.springframework.boot.loader.JarLauncher --scenario=ServiceBusProcessorScenario

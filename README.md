# Prerequisite

1. [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli).
2. [Docker](https://docs.docker.com/desktop/install/windows-install/).
3. [IntelliJ]( https://www.jetbrains.com/idea/download/?section=windows) with [Docker plugin]( https://www.jetbrains.com/help/idea/docker.html#install_docker) (the plugin is available by default in IntelliJ IDEA Ultimate).
4. Java 11+

# Running locally in Docker

1. Clone the repo.
2. Create the file `settings.env` in the root directory (i.e. `./servicebus-concurrentcalls-cores` ) with the following content:
```
AZURE_SERVICEBUS_CONNECTION_STRING=<connection-string>
AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES=<topic>:<subscription>;<topic>:<subscription>;<topic>:<subscription>
```

*  Update `<connection-string>` with Azure Service Bus connection string.
*  Update `<topic>:<subscription>` pair with existing topic and subscription names. You can specify multiple topic-subscription pairs separated by a ";".
3. Open the directory `servicebus-concurrentcalls-cores` in IntelliJ.
4. From the IntelliJ terminal switch to `messaging-client-scenarios` and package the Java App
      > C:\code\servicebus-concurrentcalls-cores> cd messaging-client-scenarios

      > C:\code\servicebus-concurrentcalls-cores\messaging-client-scenarios> mvn clean package spring-boot:repackage
5. Right-click on the `docker-compose.yml` in IntelliJ Project View and select `Run 'docker-compose.yml: …'`

# Running in AKS

## Prerequisite

1. AKS cluster with Azure container registry (ACR) linked.
2. The developer machine has the `kubectl` tool installed and configured to connect to the AKS cluster. 

If the prerequisites are not met, refer to [Setup AKS](./SETUP-AKS-README.md).

3. Define an env variable `container_registry` (scoped to the current Windows command Prompt) with the name of the existing ACR associated with the AKS cluster.

```
set container_registry=contosoacr
```

## Create a namespace in AKS cluster

```
set aks_namespace=contoso-sb-app-ns
```

```
kubectl create namespace %aks_namespace%
```

## Assign secrets to the AKS namespace

### Create secrets.yml

```yaml
apiVersion: v1
data:
   AZURE_SERVICEBUS_CONNECTION_STRING: <base65-encode(servicebus-connection-string)>
   AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES: <base65-encode(<topic>:<subscription>;<topic>:<subscription>)>
kind: Secret
metadata:
   name: java-sb-app-secret
   namespace: <aks_namespace>
type: Opaque
```

* Update `<aks-namespace>` to use the aks namespace we created above (e.g., `contoso-sb-app-ns`).
* Update with the base64 encoded Service Bus connection string and the topic-subscription pairs.

#### Base64 encoding Service Bus connection string and the topic-subscription pairs

> Below we used WSL shell in Windows but any Linux (E.g. Git Bash) shell will do

Input to echo should be in single quotes

```
echo '<servicebus-connection-string>' | base64
```

Use the output to replace `<base65-encode(servicebus-connection-string)>`.

```
echo '<topic>:<subscription>;<topic>:<subscription>' | base64
```

Replace `<topic>:<subscription>` pair with existing topic and subscription names. You can specify multiple topic-subscription pairs separated by a ";".

Use the output to replace `<base65-encode(<topic>:<subscription>;<topic>:<subscription>)>`.

### Apply the secrets to the aks namespace

```
kubectl apply -f <absolute-path-to>/secrets.yml
```

## Create the Docker image with the Java App

Clone the repo, switch to `messaging-client-scenarios` directory and package the Spring Boot Java App,

> C:\code\servicebus-concurrentcalls-cores> cd messaging-client-scenarios

> C:\code\servicebus-concurrentcalls-cores\messaging-client-scenarios> mvn clean package spring-boot:repackage

and build the Docker image
```
docker build -t %container_registry%.azurecr.io/messaging-client-scenarios:latest .
```

## Push the Docker image to ACR

```
az acr login -n %container_registry%.azurecr.io

docker push %container_registry%.azurecr.io/messaging-client-scenarios:latest
```

## Deploy container based on the Docker image to AKS cluster

### Make necessary update to job.yml

The `job.yml` contains the definition for the container we want to deploy in the AKS namespace using the docker image (with messaging Spring boot App) in ACR.

Open the `job.yml` file

1. Search for 'contosoacr' and replace it with the name of the ACR to which the Docker image was pushed.
2. The value of `metadata.namespace` is 'contoso-sb-app-ns' (the AKS namespace created above).
3. The value of `spec.template.spec.nodeSelector.agentpool` is 'nodepool1' ('nodepool1' is the default AKS pool name, verify this in the portal).

### Apply job.yml to deploy container in AKS.

```
kubectl create -f <absolute-path-to>\job.yml
```

One container will be deployed as described in the `job.yml`, which runs an instance of "Java Messaging Spring Boot App" included in the Docker image. The name of the container is `processors` (as named in the job.yml).

#### Checking container deployment status

```
kubectl get pods -n %aks_namespace%
```

```
NAME                      READY   STATUS    RESTARTS   AGE
java-sb-app-h8qlp         1/1     Running   0          9s
```

The pod prefix `java-sb-app-` is derived from the value of `metadata.name` in job.yml.
It shows the container (1/1) are ready and running.

#### Redirecting the stdout of container in the pod

If the Java program write logs to stdout (system.out.println), that can be redirected to your terminal,

```
kubectl logs -n %aks_namespace% java-sb-app-h8qlp -c processors -f
```

The value of -c option is the name for the container as defined in job.yml.

#### Accessing the container shell

```
kubectl exec -n %aks_namespace% java-sb-app-h8qlp --container processors -it -- /bin/bash
```

You may check number of cores from this shell using the following command

```
root [ /application ]# jshell
MM DD, YYYY H:M:S java.util.prefs.FileSystemPreferences$1 run
INFO: Created user preferences directory.
|  Welcome to JShell -- Version 11.0.24
|  For an introduction type: /help intro

jshell> Runtime.getRuntime().availableProcessors();
$1 ==> 2
```

#### Copying slf4j log files

The Java Spring App has log4j logging enabled; the following command copies the log files to your machine from the pod container `processors`

```
kubectl cp  -n %aks_namespace% -c processors java-sb-app-h8qlp:/application/logs/app.log app.log
```

### Deleting the pod and containers

to delete the deployment run the following command

```
kubectl delete -f <absolute-path-to>\job.yml
```

# Prerequisite

1. [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli).
2. [Docker](https://docs.docker.com/desktop/install/windows-install/).
3. [IntelliJ]( https://www.jetbrains.com/idea/download/?section=windows) with [Docker plugin]( https://www.jetbrains.com/help/idea/docker.html#install_docker) (the plugin is available by default in IntelliJ IDEA Ultimate).
4. Java 11+

# Running locally in docker

1. Clone the repo.
2. Create the file `settings.env` in the root directory (i.e. `./servicebus-concurrentcalls-cores` ) with the following content:
```
AZURE_SERVICEBUS_CONNECTION_STRING=<connection-string>
AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES=<topic>:<subscription>;<topic>:<subscription>;<topic>:<subscription>
```

*  Replace `<connection-string>` with the connection string of your Azure Service Bus namespace.
*  Replace `<topic>:<subscription>` with the topic and subscription names for the test. You can specify multiple topic-subscription pairs separated by a ";".
3. Open the folder `servicebus-concurrentcalls-cores` in IntelliJ.
4. From the IntelliJ terminal switch to `messaging-client-scenarios` and package the Java App
      > C:\code\servicebus-concurrentcalls-cores> cd messaging-client-scenarios

      > C:\code\servicebus-concurrentcalls-cores\messaging-client-scenarios> mvn clean package spring-boot:repackage
5. Right-click on the `docker-compose.yml` and select `Run 'docker-compose.yml: …'`


# Running in AKS

> This section presumes that you have an AKS cluster with a Azure container registry (ACR) linked to it. Additionally, it assumes that the developer machine has the `kubectl` tool installed and configured to connect to the AKS cluster. If any of these prerequisites are not met, please refer to [SETUP-AKS-README.md](./SETUP-AKS-README.md).

Define an env variable `container_registry` (scoped to the current Windows command Prompt) with the name of the existing ACR associated with the AKS cluster.

```
set container_registry=contosoacr
```

## Create AKS namespace and assign secrets

```
set aks_namespace=contoso-sb-app-ns
```

```
kubectl create namespace %aks_namespace%
```

### Create secrets.yml file locally

```yaml
apiVersion: v1
data:
   AZURE_SERVICEBUS_CONNECTION_STRING: <base65-encoded(servicebus-connection-string)>
   AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES: <base65-encoded(<topic>:<subscription>;<topic>:<subscription>)>
kind: Secret
metadata:
   name: java-sb-app-secret
   namespace: <aks_namespace>
type: Opaque
```

Update `<aks-namespace>` to use the aks namespace we created above (e.g., `contoso-sb-app-ns`)

The metatdata.name value i.e. 'java-sb-app-secret' is an identifier for the secrets; this identifier will be referenced from aks `job.yml` definition (more on that later).

### Base64 encode the secrets

Base 64 encode the secrets i.e. Service Bus connection string and the topic-subscription pairs.

> Below we used WSL shell in Windows but any linux (E.g. Git Bash) shell will do

Input to echo should be in single quotes

```
echo '<servicebus-connection-string>' | base64
```

Use the output to replace `<base65-encoded(servicebus-connection-string)>` in secrets.yml

```
echo '<topic>:<subscription>;<topic>:<subscription>' | base64
```

Replace `<topic>:<subscription>` pair with the topic and subscription names for the test. You can specify multiple topic-subscription pairs separated by a ";".

Use the output to replace `<base65-encoded(<topic>:<subscription>;<topic>:<subscription>)>` in secrets.yml

### Assign the secrets to the aks namespace

```
kubectl apply -f <absolute-path-to>/secrets.yml
```

## Create the docker image with messaging Spring boot App

Clone the repo, switch to `messaging-client-scenarios` directory and package the Java App

> C:\code\servicebus-concurrentcalls-cores> cd messaging-client-scenarios

> C:\code\servicebus-concurrentcalls-cores\messaging-client-scenarios> mvn clean package spring-boot:repackage

and build the docker image
```
docker build -t %container_registry%.azurecr.io/messaging-client-scenarios:latest .
```

## Push the docker image to ACR linked to the AKS

```
az acr login -n %container_registry%.azurecr.io

docker push %container_registry%.azurecr.io/messaging-client-scenarios:latest
```

## Deploy to AKS cluster

### Make necessary update to job.yml

The `job.yml` contains the definition for the containers we want to deploy in the aks namespace using the docker image (with the java spring app) we pushed to acr.

Open the `job.yml` file

1. The value of `metadata.namespace` is 'contoso-sb-app-ns'; replace it with the aks namespace created above.
2. The value of `spec.template.spec.nodeSelector.agentpool` is 'nodepool1'; replace it with the aks pool name appear in the portal.
3. Search for `image:`, its value is 'contosoacr.azurecr.io/messaging-client-scenarios:latest', replace it with the name of the docker image we pushed to ACR earlier.

> You can also see how the secrets AZURE_SERVICEBUS_CONNECTION_STRING and AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES those created earlier (via secrets.yml) is referenced in this job.yml.

### Apply job.yml to deploy containers.

One container will be deployed as described in the `job.yml`, which runs an instance of "Java Messaging Spring App" included in the docker image.

The name of the container is `processors` (as named in the job.yml).

```
kubectl create -f <absolute-path-to>\job.yml
```

#### Checking container deployment status

```
kubectl get pods -n %aks_namespace%
```

* output:
```
NAME                      READY   STATUS    RESTARTS   AGE
java-sb-app-h8qlp         1/1     Running   0          9s
```

The pod prefix `java-sb-app-` is derived from the value of `metadata.name` in job.yml.
It shows the container (1/1) are ready and running.

#### Redirecting the stdout of container in the pod

If the Java program is writing to stdout (system.out.println), that can be redirected to your terminal using the below command.

The value of -c option is the name for the container (defined in job.yml).

```
kubectl logs -n %aks_namespace% java-sb-app-h8qlp -c processors -f
```

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

The Java Spring App has log4j logging enabled; the following command shows how to copy log files to your machine from the pod container `processors`.

```
kubectl cp  -n %aks_namespace% -c processors java-sb-app-h8qlp:/application/logs/app.log app.log
```

### Deleting the pod and containers

to delete the deployment run the following command

```
kubectl delete -f <absolute-path-to>\job.yml
```

# Setup AKS Cluster

### The env vars for Az CLI commands

Define env variables (scoped to the current Windows command Prompt) that the Azure CLI commands refers later.

Make sure to provide appropriate values for these env vars

```
set resource_group=anuchan-contoso-rg-aks
set location=eastus
set container_registry=contosoacr
set aks_cluster=contoso-aks
```

### Azure CLI commands to create resources (resource group, container registry, aks cluster)
```
az group create --name=%resource_group% --location=%location%

az acr create --resource-group %resource_group% --location %location% --name %container_registry% --sku Basic

az config set defaults.acr=%container_registry%
az acr login

az aks create --resource-group=%resource_group% --name=%aks_cluster% --dns-name-prefix=%aks_cluster% --attach-acr %container_registry% --generate-ssh-keys
```


### Install kube CLI (kubectl, kubelogin)

```
az aks install-cli
```

> Required to add 'C:\Users\<user-name>\.azure-kubectl' and 'C:\Users\<user-name>\.azure-kubelogin' to PATH. Output of the command has the instructions.

### Connect kube CLI to the aks cluster

```
az aks get-credentials --resource-group=%resource_group% --name=%aks_cluster% --overwrite
```

> This command sets the aks cluster as current context in C:\Users\<user-name>\.kube\config
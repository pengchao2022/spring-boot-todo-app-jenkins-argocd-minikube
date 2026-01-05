# spring-boot-todo-app-jenkins-argocd-minikube

In this demo, I will use github and jenkins, argocd, minikube to deploy a spring-boot todo app, It's just a simple website with mysql database, All I want to do is to show you the CICD process.

In this tutorial you will also have a small trip to know Cloud service(Alibaba cloud), docker hub service,docker container services, Kubernetes(minikube), helm charts, sonarqube, database,trivy, public network, private network and DNS and image version control.

I will create all from the very beginning.

## Functional systems

- Github ---- code and helm charts repository

- Jenkins ---- CI Continues Intergration

- ArgoCD  ---- CD Continues Delivery or Continues Deploy

- Kubernets ---- minikube in this tutorial

- Helm charts --- manage the kubernetes apps package

- ...

## Infrastructure prepare

- Alibaba ECS 1 (4 cpus, 8GB memory)

    - Install jenkins with master and agent node 

    - Install sonarqube as a code scan server

    ![jenkins-master-agent-sonar](./jenkins-sonar.png)


# Config Map Demo

## Overview

This project sets up a severely basic demo of how information can be brought into a containerized application via Kubernetes shared config maps. The approaches include:

- environment variable values read into application.properties
- a virtual properties file mounted to /etc/config and optionally read in by the application's main class

The app was written with Spring Boot and Thymeleaf. It's not pretty-fied. It's just a demo/PoC.

The documentation below is mostly cut/pasted from some minikube notes saved in a different repository. I've already put too much work into writing stuff here, so I'll just let the rest stand as-is. 

The docs don't say anything about the actual Java code. Sorry. I suggest looking at application.properties to see how I bring in the environment variable and the main application class to see how I bring in the virtual properties file. The deployment.yaml file is, of course, important for showing how the config map data is brought into the container.

The config map that I applied is in extras/. Docs on config maps are everywhere so I won't explain the file's contents here.

## Pre-requisites

- Docker installed and running (service AND socket)
    - ````systemctl restart docker.service && systemctl restart docker.socket````
- you are in the docker group
    - ````sudo usermod -aG docker $USER````
- minikube installed
    - ````curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64 && sudo install minikube-linux-amd64 /usr/local/bin/minikube````

## Start minikube

Run the following command to make Docker available on the hjost machine's terminal:

````eval $(minikube docker-env)````

When done playing, if you want to reverse what you just did:
    
````eval $(minikube docker-env -u)````

Start minikube:

````minikube start````

From here, minikube will take a few minutes to download and configure your single-node Kubernetes cluster. Once it's done, you can start deploying containers.

Worth re-mentioning: if you don't have Docker set up and running (very important) and you don't have a context selected (default is fine), minikube on Debian will start with QEMU which is not only slow, but also prevents some minikube functions from being available.

Force the issue if necessary by specifying the Docker driver on the minikube command line (or omit and just try starting minikube and see what it does):

````minikube start --driver=docker````

## Build your image

Change to the directory in your application's source tree where your Dockerfile is located (find instructions on setting up this file somewhere else. Too much to go into here).

Build your image by running the following command:

````docker build -t my-image:latest .````

The image name is what is reported at the end of the Docker image build process. You will
need this. Save it somewhere. Note the last "naming to..." line below:

````
=> exporting to image
=> => exporting layers
=> => writing image sha256:9cfbc6d00433bbf63524efa1ea5396190b488213da9de9f181ef0372ac46a751
=> => naming to docker.io/library/configmapdemo:0.0.1
````

## Push to a registry (Docker Hub example...yeah, you'll need an account)

If you want to push your image to a registry (and it seems to be a pretty important step, so 
just do it), you can tag and push the image with the following command:

````docker tag my-image:latest my-dockerhub-username/my-image:latest````

````docker push my-dockerhub-username/my-image:latest````

## Get a container in minikube

Start by creating a deployment.yaml file that describes the deployment. An example:

````
apiVersion: apps/v1
kind: Deployment
metadata:
  name: configmapdemo-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: configmapdemo
  template:
    metadata:
      labels:
        app: configmapdemo
    spec:
      containers:
        - name: configmapdemo-container
          image: docker.io/library/configmapdemo:0.0.1
          # brings key:value pairs in as envars from configmap
          envFrom:
          - configMapRef:
              name: my-literal-config
          # brings key:value pairs in as a properties file from configmap (see also: volumes)
          volumeMounts:
          - name: configmap-volume
            mountPath: /etc/config
          ports:
          - containerPort: 8080
          resources:
            limits:
              cpu: "1"
              memory: "512Mi"
            requests:
              cpu: "0.5"
              memory: "256Mi"
      volumes:
        - name: configmap-volume
          configMap: 
            name: my-literal-config
````

Then, apply the deployment.yaml:

````kubectl apply -f deployment.yaml````

This will create a deployment with a single pod. The deployment will use the image you built in the previous step. Note: during testing, this somehow made a flimsy deployment that would not run. I had to delete the deployment and re-create it to get it to work. That should not be the case. There needs to be a way to deploy with deployment.yaml, not the command line below, in order to control bringing in configmap data.

Alternate method of deploying, sans deployment.yaml (the image name above goes in this command line):

````kubectl create deployment configmapdemo-deployment --image=docker.io/library/configmapdemo:0.0.1````

Expose the deployment:

````kubectl expose deployment configmapdemo-deployment --type=NodePort --port=8080````

Get the URL:

````minikube service configmapdemo-deployment --url````

The URL that is returned is the one that should get you to your running web app.

This last command is where most headaches seem to appear. If you get a "service not found" error, you may need to delete the service and re-create it. Simply deleting and re-applying the deployment will not fix the issue. Example of how to delete a service (use ````kubectl get services```` to get the service name):

````kubectl delete service configmapdemo-deployment````

## Fun with kubernetes

Time to play.

### Add a shared ConfigMap

Create a ConfigMap:

````
apiVersion: v1
kind: ConfigMap
metadata:
    name: my-literal-config
data:
    DEMO_ENVIRONMENT_STRING: "This is from an environment variable in a ConfigMap"
    configmapdemo.properties: |
        demo.file.string=This is from a file in a ConfigMap
````    

Apply the ConfigMap:

````kubectl apply -f configmapfile.yaml````

Add the ConfigMap to a deployment (NOTE: The property file instructions below are not working yet. The Docker image build will fail because the property file will not be found. Hold off on adding property files via ConfigMaps!):

````
...
spec:
    containers:
        - name: configmapdemo-container
          volumeMounts:
            - name: configmap-volume
              mountPath: /etc/config
    image: configmapdemo
    imagePullPolicy: Never
    envFrom:
    - configMapRef:
        name: my-literal-config
    volumes:
        - name: configmap-volume
          configMap: 
            name: my-literal-config
...
````

Note the volumeMounts and volumes sections for the file item and the envFrom section for the environment variable.

In Java/Spring, be sure to add a property source annotation in the main application class that reads in the file from the ConfigMap:

````
@PropertySource("classpath:configmapdemo.properties")
````

### Log into running pod

````kubectl exec podname -it -- /bin/bash````

### TODO

1. Go through this document and make config map file names consistent.
2. While you're at it, make sure deployment names, Docker image file names, etc. are consistent. What a mess.
3. Add a section that goes over troubleshooting.
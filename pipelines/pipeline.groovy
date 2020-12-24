pipeline {
  agent {
    kubernetes {
      defaultContainer 'jnlp'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: 'maven'
      image: maven:3.3.9
      command:
        - cat
      tty: true
      volumeMounts:
      - mountPath: "/root/.m2/repository"
        name: "maven-cache"
        readOnly: false
    - name: 'dind'
      image: docker:dind
      securityContext:
        privileged: true
    - name: 'kubectl'
      image: tiborv/aws-kubectl
      command:
        - cat
      tty: true
  volumes:
  - hostPath:
      path: "/root/.m2/repository"
    name: "maven-cache"
"""
    }
  }

  environment {
    VERSION = ""
    DOCKER_CREDENTIALS = credentials('DOCKER_CREDENTIALS')
    KUBE_CONFIG = credentials('KUBE_CONFIG')
  }

  stages {

    

    // Insert here

    stage('Deploy'){
      steps {
        container('kubectl') {
          // writeFile file: "/tmp/.kube/config", text: readFile(KUBE_CONFIG)
          sh ("kubectl --kubeconfig $KUBE_CONFIG get pods")
            // export KUBECONFIG=/tmp/.kube/config
            // sed -i 's/IMAGE_TAG/${VERSION}/g' deploy.yaml
            // kubectl apply -f deploy.yaml
            // kubectl get svc spring-petclinic
          // """
        }
      }
    }  

  }

}


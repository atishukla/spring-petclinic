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
    KUBERNETES_TOKEN = credentials('KUBERNETES_TOKEN')
    KUBERNETES_SERVER = credentials('KUBERNETES_SERVER')
    KUBERNETES_CLUSTER_CERTIFICATE = credentials('KUBERNETES_CLUSTER_CERTIFICATE')
  }

  stages {

    

    stage('Deploy'){
      steps {
        container('kubectl') {
          sh"""
            echo ${KUBERNETES_CLUSTER_CERTIFICATE} > cert.crt
            cat cert.crt
            kubectl \
            --insecure-skip-tls-verify \
            --kubeconfig="/dev/null" \
            --server=${KUBERNETES_SERVER} \
            --token=${KUBERNETES_TOKEN} \
            get pods
          """
        }
      }
    }  

  }

}


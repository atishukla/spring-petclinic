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
    - name: 'dind'
      image: docker:dind
      securityContext:
        privileged: true
    - name: 'kubectl'
      image: bitnami/kubectl
      command:
        - cat
      tty: true
"""
    }
  }

  environment {
    VERSION = ""
  }

  stages {
    stage('Checkout') {
      steps {
        script {
          deleteDir()
          checkout scm
          def matcher = readFile('pom.xml') =~ '<version>(.+?)</version>'
          def current_version = matcher ? matcher[0][1] : '0.1.0'
          VERSION = current_version+'.'+BUILD_NUMBER
          echo ${VERSION}
        }
      }
    }
  }

}


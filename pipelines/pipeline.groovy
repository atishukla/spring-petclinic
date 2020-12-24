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
      image: bitnami/kubectl
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
    KUBE_CONFIG = credentials('KUBE_CONFIG ')
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
          echo "Version is ${VERSION}"
        }
      }
    }

    stage('Compile') {
      steps {
        container('maven') {
          script {
            sh """
              mvn versions:set -DnewVersion=${VERSION}
              mvn clean compile
            """
          }
        }
      }
    }

    stage('Test') {
      steps {
        container('maven') {
          sh "mvn verify -DskipTests"
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Build Image') {
      steps {
        container('dind') {
          sh """
            mkdir target/working-dir
            cp -R docker/Dockerfile target/working-dir/
            cp target/spring-petclinic*.jar target/working-dir/
            cd target/working-dir
            docker build -t atishayshukla/spring-petclinic:${VERSION} .
          """
        }
      }
    }

    stage('Push Image'){
      steps {
        container('dind') {
          sh """
            docker login -u ${DOCKER_CREDENTIALS_USR} -p ${DOCKER_CREDENTIALS_PSW}
            docker push atishayshukla/spring-petclinic:${VERSION}
          """
        }
      }
    }

    stage('Deploy'){
      steps {
        container('kubectl') {
          writeFile file: "/tmp/.kube/config", text: readFile(KUBE_CONFIG)
          sh """
            export KUBECONFIG=/tmp/.kube/config
            sed -i 's/IMAGE_TAG/${VERSION}/g' deploy.yaml
            kubectl apply -f deploy.yaml
            kubectl get svc spring-petclinic
          """
        }
      }
    }  

  }

}


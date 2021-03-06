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
            echo ${DOCKER_CREDENTIALS_PSW} | docker login -u ${DOCKER_CREDENTIALS_USR} --password-stdin
            docker push atishayshukla/spring-petclinic:${VERSION}
          """
        }
      }
    }

    stage('Deploy'){
      steps {
        container('kubectl') {
          sh"""
            sed -i 's/IMAGE_TAG/${VERSION}/g' deploy.yaml
            kubectl \
            --insecure-skip-tls-verify \
            --kubeconfig="/dev/null" \
            --server=${KUBERNETES_SERVER} \
            --token=${KUBERNETES_TOKEN} \
            apply -f deploy.yaml
            kubectl \
            --insecure-skip-tls-verify \
            --kubeconfig="/dev/null" \
            --server=${KUBERNETES_SERVER} \
            --token=${KUBERNETES_TOKEN} \
            get svc spring-petclinic
          """
        }
      }
    }  

  }

}


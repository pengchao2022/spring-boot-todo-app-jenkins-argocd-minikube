pipeline {
    agent {
        label 'jenkins-agent'
    }
    
    environment {
        DOCKER_HUB_USER = 'pengchaoma'
        DOCKER_HUB_CREDENTIALS = 'dockerhub-creds'
        IMAGE_NAME = 'spring-todo-app'
        IMAGE_TAG = "build-${BUILD_NUMBER}"
        IMAGE_FULL = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
        IMAGE_LATEST = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
        JAVA_HOME = '/opt/java/openjdk'
        MAVEN_HOME = '/home/jenkins/tools/maven/apache-maven-3.9.12'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'ls -la'
            }
        }
        
        stage('Setup Environment') {
            steps {
                sh """
                    export PATH=${MAVEN_HOME}/bin:${JAVA_HOME}/bin:\\\$PATH
                    java -version
                    mvn -version
                """
            }
        }
        
        stage('Compile') {
            steps {
                sh """
                    export PATH=${MAVEN_HOME}/bin:${JAVA_HOME}/bin:\\\$PATH
                    mvn clean compile
                """
            }
        }
        
        stage('Test') {
            steps {
                sh """
                    export PATH=${MAVEN_HOME}/bin:${JAVA_HOME}/bin:\\\$PATH
                    mvn test
                """
            }
        }
        
        stage('Package') {
            steps {
                sh """
                    export PATH=${MAVEN_HOME}/bin:${JAVA_HOME}/bin:\\\$PATH
                    mvn package -DskipTests
                """
            }
        }
        
        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${IMAGE_FULL} .
                    docker tag ${IMAGE_FULL} ${IMAGE_LATEST}
                """
            }
        }
        
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKER_HUB_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh """
                        echo "\${DOCKER_PASSWORD}" | docker login -u "\${DOCKER_USER}" --password-stdin
                        docker push ${IMAGE_FULL}
                        docker push ${IMAGE_LATEST}
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo "Build ${BUILD_NUMBER} success"
            echo "Image: ${IMAGE_FULL}"
        }
        failure {
            echo "Build ${BUILD_NUMBER} failed"
        }
        always {
            sh 'docker logout 2>/dev/null || true'
        }
    }
}
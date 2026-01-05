pipeline {
    agent {
        label 'jenkins-agent'
    }
    
    tools {
        maven 'maven'
    }
    
    environment {
        DOCKER_HUB_USER = 'pengchaoma'
        DOCKER_HUB_CREDENTIALS = 'dockerhub-creds'
        IMAGE_NAME = 'springboot-todo-app'
        IMAGE_TAG = "${BUILD_NUMBER}"
        IMAGE_FULL = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
        IMAGE_LATEST = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }
        
        stage('Build with Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        docker build -t ${IMAGE_FULL} .
                        docker tag ${IMAGE_FULL} ${IMAGE_LATEST}
                    """
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "${DOCKER_HUB_CREDENTIALS}",
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
                            docker push ${IMAGE_FULL}
                            docker push ${IMAGE_LATEST}
                            docker logout
                        """
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ Build successful!"
            echo "Images pushed to Docker Hub:"
            echo "- ${IMAGE_FULL}"
            echo "- ${IMAGE_LATEST}"
        }
        failure {
            echo "❌ Build failed!"
        }
    }
}
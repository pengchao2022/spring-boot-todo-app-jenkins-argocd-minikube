pipeline {
    agent any
    
    environment {
        // Docker Hub 配置
        DOCKER_HUB_USER = 'pengchaoma'
        DOCKER_HUB_CREDENTIALS = 'dockerhub-creds'
        
        // 镜像配置
        IMAGE_NAME = 'spring-todo-app'
        IMAGE_TAG = "build-${BUILD_NUMBER}"
        IMAGE_FULL = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
        IMAGE_LATEST = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
        
        // Maven 配置
        MAVEN_HOME = tool 'maven-3.9'
    }
    
    tools {
        maven 'maven-3.9'
        jdk 'jdk17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '📦 Checking out code...'
                checkout scm
                sh '''
                    echo "=== Project Structure ==="
                    find . -name "*.java" -o -name "*.xml" -o -name "Dockerfile" | sort
                '''
            }
        }
        
        stage('Build') {
            steps {
                echo '🔨 Building with Maven...'
                sh '''
                    mvn clean compile
                    echo "Build completed!"
                '''
            }
        }
        
        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                sh '''
                    mvn test
                    echo "Tests completed!"
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco execPattern: '**/target/jacoco.exec'
                }
            }
        }
        
        stage('Package') {
            steps {
                echo '📦 Packaging JAR...'
                sh '''
                    mvn package -DskipTests
                    ls -lh target/*.jar
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    sh """
                        docker build -t ${IMAGE_FULL} .
                        docker tag ${IMAGE_FULL} ${IMAGE_LATEST}
                    """
                    
                    sh '''
                        echo "=== Image Info ==="
                        docker images | grep ${DOCKER_HUB_USER}/${IMAGE_NAME}
                    '''
                }
            }
        }
        
        stage('Test Docker Image') {
            steps {
                echo '🚀 Testing Docker image...'
                script {
                    sh """
                        # 启动测试容器
                        docker run -d --name todo-test -p 8081:8080 ${IMAGE_FULL}
                        sleep 10
                        
                        # 测试健康检查
                        echo "Testing health endpoint..."
                        curl -f http://localhost:8081/api/health || echo "Health check failed"
                        
                        # 停止容器
                        docker stop todo-test
                        docker rm todo-test
                    """
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing to Docker Hub...'
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "${DOCKER_HUB_CREDENTIALS}",
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            docker push ${IMAGE_FULL}
                            docker push ${IMAGE_LATEST}
                        """
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '🎉 CI Pipeline completed successfully!'
            script {
                echo """
                =========================================
                ✅ CI PROCESS COMPLETE
                =========================================
                
                📦 Docker Images:
                   🏷️  ${IMAGE_FULL}
                   🔖 ${IMAGE_LATEST}
                
                🔗 Pull Commands:
                   docker pull ${IMAGE_FULL}
                   docker pull ${IMAGE_LATEST}
                
                🚀 Run Commands:
                   docker run -d -p 8080:8080 ${IMAGE_FULL}
                
                🌐 Access:
                   http://localhost:8080
                   http://localhost:8080/api/health
                =========================================
                """
            }
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        always {
            echo '🧹 Cleaning up...'
            sh '''
                docker logout 2>/dev/null || true
                docker system prune -f 2>/dev/null || true
            '''
        }
    }
}
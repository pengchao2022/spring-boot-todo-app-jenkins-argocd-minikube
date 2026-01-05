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
        
        // SonarQube 配置
        SONAR_HOST_URL = 'http://sonarqube.awsmpc.asia:9000'
        SONAR_PROJECT_KEY = 'spring-boot-todo-app'
        SONAR_PROJECT_NAME = 'Spring Boot Todo Application'
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }
        
        stage('Verify Environment') {
            steps {
                sh '''
                    echo "=== Environment Verification ==="
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "Workspace: $(pwd)"
                    echo ""
                    echo "=== Java and Maven ==="
                    java -version
                    mvn --version
                    echo ""
                    echo "=== Docker ==="
                    docker --version
                    echo ""
                    echo "=== Project Structure ==="
                    find . -name "*.java" | head -10
                '''
            }
        }
        
        stage('SonarQube Code Analysis') {
            steps {
                script {
                    echo "=== Starting SonarQube Analysis ==="
                    
                    // 使用我们测试成功的 Token 凭据
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        sh """
                            echo "SonarQube URL: ${SONAR_HOST_URL}"
                            echo "Project Key: ${SONAR_PROJECT_KEY}"
                            echo "Project Name: ${SONAR_PROJECT_NAME}"
                            echo ""
                            
                            # 先编译代码
                            mvn clean compile -DskipTests
                            
                            # 运行 SonarQube 扫描
                            mvn sonar:sonar \
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                              -Dsonar.projectName='${SONAR_PROJECT_NAME}' \
                              -Dsonar.host.url=${SONAR_HOST_URL} \
                              -Dsonar.login=${SONAR_TOKEN} \
                              -Dsonar.sources=src/main/java \
                              -Dsonar.java.binaries=target/classes \
                              -Dsonar.sourceEncoding=UTF-8 \
                              -Dsonar.java.source=17 \
                              -Dsonar.exclusions=**/*Test*.java,**/test/** \
                              -DskipTests
                        """
                    }
                    
                    echo "✅ SonarQube analysis submitted successfully!"
                    echo "📊 Report available at: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
                }
            }
        }
        
        stage('Build and Package') {
            steps {
                sh '''
                    echo "=== Building and packaging application ==="
                    mvn clean package -DskipTests
                    
                    echo "=== Build artifacts ==="
                    ls -lh target/*.jar
                    
                    # 显示 JAR 文件信息
                    if [ -f target/*.jar ]; then
                        JAR_FILE=$(ls target/*.jar)
                        echo "Main JAR: ${JAR_FILE}"
                        echo "Size: $(du -h ${JAR_FILE} | cut -f1)"
                        echo "Created: $(stat -c %y ${JAR_FILE})"
                    fi
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        echo "=== Building Docker image ==="
                        docker build -t ${IMAGE_FULL} .
                        docker tag ${IMAGE_FULL} ${IMAGE_LATEST}
                        
                        echo "=== Docker images created ==="
                        docker images | grep ${DOCKER_HUB_USER}/${IMAGE_NAME} || echo "No matching images found"
                        
                        # 显示镜像详情
                        echo ""
                        echo "=== Image details ==="
                        docker inspect ${IMAGE_FULL} --format='Size: {{.Size}} bytes' | awk '{print "Image size: " $1/1024/1024 " MB"}'
                    '''
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
                            echo "=== Pushing to Docker Hub ==="
                            echo "Username: ${DOCKER_USERNAME}"
                            echo "Repository: ${DOCKER_HUB_USER}/${IMAGE_NAME}"
                            echo ""
                            
                            # 登录 Docker Hub
                            echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
                            
                            # 推送镜像
                            echo "Pushing ${IMAGE_FULL}..."
                            docker push ${IMAGE_FULL}
                            
                            echo "Pushing ${IMAGE_LATEST}..."
                            docker push ${IMAGE_LATEST}
                            
                            # 登出
                            docker logout
                            
                            echo ""
                            echo "✅ Images pushed successfully!"
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo "=== Build Summary ==="
            echo "Job: ${JOB_NAME}"
            echo "Build: #${BUILD_NUMBER}"
            echo "Status: ${currentBuild.currentResult}"
            echo "Duration: ${currentBuild.durationString}"
            echo "Workspace: ${WORKSPACE}"
            echo "Build URL: ${BUILD_URL}"
        }
        success {
            echo ""
            echo "🎉🎉🎉 PIPELINE SUCCESSFUL! 🎉🎉🎉"
            echo ""
            echo "📦 DOCKER IMAGES:"
            echo "   🏷️  Versioned: ${IMAGE_FULL}"
            echo "   🔖 Latest: ${IMAGE_LATEST}"
            echo ""
            echo "🔍 CODE QUALITY:"
            echo "   📊 SonarQube Report: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
            echo ""
            echo "🚀 DEPLOYMENT COMMANDS:"
            echo "   # Pull and run the application"
            echo "   docker pull ${IMAGE_FULL}"
            echo "   docker run -d -p 8080:8080 --name todo-app ${IMAGE_FULL}"
            echo ""
            echo "   # Or use latest tag"
            echo "   docker pull ${IMAGE_LATEST}"
            echo "   docker run -d -p 8080:8080 --name todo-app-latest ${IMAGE_LATEST}"
            echo ""
            echo "🔧 HEALTH CHECK:"
            echo "   curl http://localhost:8080/actuator/health"
            echo "   curl http://localhost:8080/"
            echo ""
            echo "========================================="
        }
        failure {
            echo ""
            echo "❌❌❌ PIPELINE FAILED! ❌❌❌"
            echo ""
            echo "Possible issues to check:"
            echo "1. Maven build errors"
            echo "2. SonarQube connection issues"
            echo "3. Docker build failures"
            echo "4. Docker Hub authentication"
            echo "5. Network connectivity"
            echo ""
            echo "Check the logs above for detailed error messages."
            echo "========================================="
        }
        cleanup {
            // 清理工作空间
            cleanWs()
        }
    }
}
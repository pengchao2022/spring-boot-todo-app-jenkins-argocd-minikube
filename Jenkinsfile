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
        
        // 语义化版本配置
        MAJOR_VERSION = '1'
        MINOR_VERSION = '0'
        PATCH_VERSION = '0'  // 可以手动更新，或者从pom.xml读取
        
        // 构建版本号
        BUILD_NUMBER_SUFFIX = "${BUILD_NUMBER}"
        
        // 完整的版本标签
        VERSION_TAG = "${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}.${BUILD_NUMBER_SUFFIX}"
        SHORT_VERSION_TAG = "${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
        
        // Docker 镜像标签
        IMAGE_FULL = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${VERSION_TAG}"
        IMAGE_LATEST = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
        IMAGE_VERSION = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${SHORT_VERSION_TAG}"
        
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
        
        stage('Read Version from pom.xml') {
            steps {
                script {
                    // 从 pom.xml 读取版本号（如果项目有设置）
                    try {
                        def pom = readMavenPom file: 'pom.xml'
                        def projectVersion = pom.version
                        
                        if (projectVersion && projectVersion.contains('.')) {
                            echo "Detected version from pom.xml: ${projectVersion}"
                            
                            // 解析版本号
                            def versionParts = projectVersion.tokenize('.')
                            if (versionParts.size() >= 3) {
                                MAJOR_VERSION = versionParts[0]
                                MINOR_VERSION = versionParts[1]
                                PATCH_VERSION = versionParts[2].replaceAll('-.*', '')  // 移除快照后缀
                                
                                echo "Parsed version: ${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
                            }
                        }
                    } catch (Exception e) {
                        echo "Could not read version from pom.xml, using defaults: ${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
                    }
                    
                    // 更新环境变量
                    env.VERSION_TAG = "${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}.${BUILD_NUMBER_SUFFIX}"
                    env.SHORT_VERSION_TAG = "${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
                    env.IMAGE_FULL = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${VERSION_TAG}"
                    env.IMAGE_LATEST = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
                    env.IMAGE_VERSION = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${SHORT_VERSION_TAG}"
                    
                    echo "=== Version Information ==="
                    echo "Full Version: ${VERSION_TAG}"
                    echo "Short Version: ${SHORT_VERSION_TAG}"
                    echo "Docker Images:"
                    echo "  - ${IMAGE_FULL}"
                    echo "  - ${IMAGE_VERSION}"
                    echo "  - ${IMAGE_LATEST}"
                }
            }
        }
        
        stage('Verify Environment') {
            steps {
                sh '''
                    echo "=== Environment Verification ==="
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "Version: ${VERSION_TAG}"
                    echo "Workspace: $(pwd)"
                    echo ""
                    echo "=== Java and Maven ==="
                    java -version
                    mvn --version
                '''
            }
        }
        
        stage('Maven Compile Code') {
            steps {
                sh '''
                    echo "=== Compiling version ${VERSION_TAG} ==="
                    mvn clean compile -DskipTests
                '''
            }
        }
        
        stage('SonarQube Code Analysis') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        sh """
                            echo "=== SonarQube Analysis for ${VERSION_TAG} ==="
                            mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar \\
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                              -Dsonar.projectName='${SONAR_PROJECT_NAME} - ${VERSION_TAG}' \\
                              -Dsonar.host.url=${SONAR_HOST_URL} \\
                              -Dsonar.login=\${SONAR_TOKEN} \\
                              -Dsonar.sources=src/main/java \\
                              -Dsonar.java.binaries=target/classes \\
                              -DskipTests
                        """
                    }
                }
            }
        }
        
        stage('Package Application') {
            steps {
                sh '''
                    echo "=== Packaging version ${VERSION_TAG} ==="
                    mvn package -DskipTests
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        echo "=== Building Docker images ==="
                        echo "Building image with tags:"
                        echo "  1. ${IMAGE_FULL} (full version with build number)"
                        echo "  2. ${IMAGE_VERSION} (semantic version)"
                        echo "  3. ${IMAGE_LATEST} (latest)"
                        
                        # 构建并标记多个标签
                        docker build -t ${IMAGE_FULL} .
                        docker tag ${IMAGE_FULL} ${IMAGE_VERSION}
                        docker tag ${IMAGE_FULL} ${IMAGE_LATEST}
                        
                        echo ""
                        echo "=== Docker images created ==="
                        docker images | grep ${DOCKER_HUB_USER}/${IMAGE_NAME}
                    """
                }
            }
        }
        
        stage('Scan Docker Image with Trivy') {
            steps {
                script {
                    echo "=== Scanning Docker Image ${IMAGE_FULL} with Trivy ==="
                    
                    // 扫描完整版本的镜像
                    sh """
                        trivy image \\
                          --exit-code 1 \\
                          --severity HIGH,CRITICAL \\
                          --ignore-unfixed \\
                          --format table \\
                          ${IMAGE_FULL}
                    """
                    
                    // 同时扫描语义版本标签的镜像（可选）
                    echo "=== Scanning Docker Image ${IMAGE_VERSION} with Trivy ==="
                    sh """
                        trivy image \\
                          --exit-code 0 \\
                          --severity HIGH,CRITICAL \\
                          --ignore-unfixed \\
                          --format table \\
                          ${IMAGE_VERSION}
                    """
                    
                    echo "Trivy vulnerability scanning completed successfully."
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
                            echo "\${DOCKER_PASSWORD}" | docker login -u "\${DOCKER_USERNAME}" --password-stdin
                            
                            echo "Pushing ${IMAGE_FULL}..."
                            docker push ${IMAGE_FULL}
                            
                            echo "Pushing ${IMAGE_VERSION}..."
                            docker push ${IMAGE_VERSION}
                            
                            echo "Pushing ${IMAGE_LATEST}..."
                            docker push ${IMAGE_LATEST}
                            
                            docker logout
                            
                            echo ""
                            echo "All images pushed successfully!"
                        """
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo ""
            echo "DEPLOYMENT SUCCESSFUL!"
            echo ""
            echo "VERSION: ${VERSION_TAG}"
            echo ""
            echo "DOCKER IMAGES:"
            echo "   Full Version: ${IMAGE_FULL}"
            echo "   Semantic Version: ${IMAGE_VERSION}"
            echo "   Latest: ${IMAGE_LATEST}"
            echo ""
            echo "SECURITY SCAN:"
            echo "   Trivy scan completed - No HIGH/CRITICAL vulnerabilities found"
            echo ""
            echo "CODE QUALITY:"
            echo "   SonarQube: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
            echo ""
            echo "DEPLOYMENT OPTIONS:"
            echo "   # For production (specific version)"
            echo "   docker pull ${IMAGE_VERSION}"
            echo "   docker run -d -p 8080:8080 --name todo-app-${SHORT_VERSION_TAG} ${IMAGE_VERSION}"
            echo ""
            echo "   # For testing (with build number)"
            echo "   docker pull ${IMAGE_FULL}"
            echo "   docker run -d -p 8080:8080 --name todo-app-build-${BUILD_NUMBER} ${IMAGE_FULL}"
            echo ""
            echo "   # For development (latest)"
            echo "   docker pull ${IMAGE_LATEST}"
            echo "   docker run -d -p 8080:8080 --name todo-app-latest ${IMAGE_LATEST}"
        }
        failure {
            echo ""
            echo "BUILD FAILED!"
            echo "Possible reasons:"
            echo "   1. Code compilation failed"
            echo "   2. Unit tests failed"
            echo "   3. SonarQube quality gate failed"
            echo "   4. Trivy found HIGH or CRITICAL vulnerabilities in the Docker image"
            echo ""
            echo "For security vulnerabilities, check the Trivy scan output above."
            echo "If vulnerabilities need to be addressed, fix the Dockerfile or base image."
        }
    }
}
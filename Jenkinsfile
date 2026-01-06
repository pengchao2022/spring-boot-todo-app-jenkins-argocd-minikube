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
        
        // semantic version control,
        MAJOR_VERSION = '1'
        MINOR_VERSION = '0'
        PATCH_VERSION = '0' 
        
        // build version
        BUILD_NUMBER_SUFFIX = "${BUILD_NUMBER}"
        
        // full version tag
        VERSION_TAG = "${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}.${BUILD_NUMBER_SUFFIX}"
        SHORT_VERSION_TAG = "${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
        
        // Docker image tags
        IMAGE_FULL = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${VERSION_TAG}"
        IMAGE_LATEST = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
        IMAGE_VERSION = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${SHORT_VERSION_TAG}"
        
        // SonarQube configuration
        SONAR_HOST_URL = 'http://sonarqube.awsmpc.asia:9000'
        SONAR_PROJECT_KEY = 'spring-boot-todo-app'
        SONAR_PROJECT_NAME = 'Spring Boot Todo Application'
        
        // email notification
        EMAIL_RECIPIENTS = '18510656167@163.com'
        EMAIL_FROM = 'pengchao.ma6@gmail.com'
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
                    // 从 pom.xml read version number
                    try {
                        def pom = readMavenPom file: 'pom.xml'
                        def projectVersion = pom.version
                        
                        if (projectVersion && projectVersion.contains('.')) {
                            echo "Detected version from pom.xml: ${projectVersion}"
                            
                            // get major, minor, patch
                            def versionParts = projectVersion.tokenize('.')
                            if (versionParts.size() >= 3) {
                                MAJOR_VERSION = versionParts[0]
                                MINOR_VERSION = versionParts[1]
                                PATCH_VERSION = versionParts[2].replaceAll('-.*', '')  
                                
                                echo "Parsed version: ${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
                            }
                        }
                    } catch (Exception e) {
                        echo "Could not read version from pom.xml, using defaults: ${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}"
                    }
                    
                    // update environment variables
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
        
        stage('Trivy Scan Docker Image') {
            steps {
                script {
                    echo "=== Scanning Docker Image ${IMAGE_FULL} with Trivy ==="
                    
                    // scan image and generate report
                    sh """
                        trivy image \\
                          --exit-code 0 \\
                          --severity HIGH,CRITICAL \\
                          --ignore-unfixed \\
                          --format table \\
                          --output trivy-report-${BUILD_NUMBER}.txt \\
                          ${IMAGE_FULL}
                        
                        # count vulnerabilities
                        CRITICAL_COUNT=\$(grep -c "CRITICAL" trivy-report-${BUILD_NUMBER}.txt 2>/dev/null || echo "0")
                        HIGH_COUNT=\$(grep -c "HIGH" trivy-report-${BUILD_NUMBER}.txt 2>/dev/null || echo "0")
                        TOTAL_VULNS=\$((CRITICAL_COUNT + HIGH_COUNT))
                        
                        echo "=== Security Scan Results ==="
                        echo "CRITICAL vulnerabilities: \$CRITICAL_COUNT"
                        echo "HIGH vulnerabilities: \$HIGH_COUNT"
                        echo "Total (HIGH+CRITICAL): \$TOTAL_VULNS"
                        
                        # 保存统计信息到文件
                        echo "TRIVY_CRITICAL=\$CRITICAL_COUNT" > trivy-stats.txt
                        echo "TRIVY_HIGH=\$HIGH_COUNT" >> trivy-stats.txt
                        echo "TRIVY_TOTAL=\$TOTAL_VULNS" >> trivy-stats.txt
                    """
                    
                    // output results to environment variables
                    def stats = readProperties file: 'trivy-stats.txt'
                    env.TRIVY_CRITICAL = stats.TRIVY_CRITICAL ?: "0"
                    env.TRIVY_HIGH = stats.TRIVY_HIGH ?: "0"
                    env.TRIVY_TOTAL = stats.TRIVY_TOTAL ?: "0"
                    
                    echo "Trivy scan completed. Found ${TRIVY_TOTAL} vulnerabilities."
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
                            echo "✅ All images pushed successfully!"
                        """
                    }
                }
            }
        }
        
        stage('Send Email Notification') {
            steps {
                script {
                    echo "=== Sending Email Notification ==="
                    echo "Recipient: ${EMAIL_RECIPIENTS}"
                    echo "From: ${EMAIL_FROM}"
                    
                    // get build details
                    def buildStatus = currentBuild.result ?: 'SUCCESS'
                    def duration = currentBuild.durationString.replace(' and counting', '')
                    def trigger = currentBuild.getBuildCauses()[0]?.shortDescription ?: "Manual trigger"
                    def startTime = currentBuild.startTimeInMillis
                    def formattedTime = new Date(startTime).format("yyyy-MM-dd HH:mm:ss")
                    
                    // send email notification
                    emailext(
                        subject: "[Jenkins CI] Spring Boot Todo App - Build #${BUILD_NUMBER} - ${buildStatus}",
                        body: """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
        .container { max-width: 800px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 2px 15px rgba(0,0,0,0.1); overflow: hidden; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }
        .header h1 { margin: 0; font-size: 26px; }
        .header p { margin: 10px 0 0; opacity: 0.9; font-size: 14px; }
        .status-box { padding: 20px; border-radius: 8px; margin: 20px; font-size: 16px; }
        .status-success { background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%); color: #155724; border-left: 5px solid #28a745; }
        .status-failure { background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%); color: #721c24; border-left: 5px solid #dc3545; }
        .info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; margin: 20px; }
        .info-card { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 15px; text-align: center; }
        .info-card h3 { margin: 0 0 10px 0; color: #495057; font-size: 13px; text-transform: uppercase; letter-spacing: 1px; }
        .info-card p { margin: 0; font-size: 18px; font-weight: bold; color: #212529; }
        .section { margin: 25px 20px; }
        .section h2 { color: #495057; border-bottom: 2px solid #e9ecef; padding-bottom: 10px; margin-bottom: 15px; font-size: 18px; }
        table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; }
        th { background: #f8f9fa; text-align: left; padding: 14px 16px; font-weight: 600; color: #495057; border-bottom: 2px solid #e9ecef; }
        td { padding: 12px 16px; border-bottom: 1px solid #e9ecef; }
        .badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; margin: 0 3px; }
        .badge-success { background: #28a745; color: white; }
        .badge-warning { background: #ffc107; color: #212529; }
        .badge-danger { background: #dc3545; color: white; }
        .badge-info { background: #17a2b8; color: white; }
        .command-box { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 18px; font-family: 'Courier New', monospace; font-size: 13px; line-height: 1.5; overflow-x: auto; margin: 10px 0; }
        .footer { background: #f8f9fa; color: #6c757d; text-align: center; padding: 20px; font-size: 12px; border-top: 1px solid #e9ecef; }
        .links { margin: 20px 0; }
        .links a { display: inline-block; margin: 0 8px; padding: 10px 18px; background: #6c757d; color: white; text-decoration: none; border-radius: 6px; font-size: 14px; transition: background 0.3s; }
        .links a:hover { background: #5a6268; }
        .security-box { padding: 15px; border-radius: 8px; margin: 15px 0; }
        .security-clean { background: #d4edda; color: #155724; border-left: 4px solid #28a745; }
        .security-warning { background: #fff3cd; color: #856404; border-left: 4px solid #ffc107; }
        .security-danger { background: #f8d7da; color: #721c24; border-left: 4px solid #dc3545; }
        .vulnerability-count { font-size: 24px; font-weight: bold; margin: 5px 0; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Spring Boot Todo App CI</h1>
            <p>Build Notification - #${BUILD_NUMBER}</p>
        </div>
        
        <div class="status-box ${buildStatus == 'SUCCESS' ? 'status-success' : 'status-failure'}">
            <h2 style="margin-top: 0; font-size: 22px;">${buildStatus == 'SUCCESS' ? '✅ BUILD SUCCESSFUL' : '❌ BUILD FAILED'}</h2>
            <p><strong>Build Duration:</strong> ${duration}</p>
            <p><strong>Triggered By:</strong> ${trigger}</p>
            <p><strong>Start Time:</strong> ${formattedTime}</p>
        </div>
        
        <div class="info-grid">
            <div class="info-card">
                <h3>Version</h3>
                <p>${VERSION_TAG}</p>
            </div>
            <div class="info-card">
                <h3>Build Number</h3>
                <p>#${BUILD_NUMBER}</p>
            </div>
            <div class="info-card">
                <h3>Job Name</h3>
                <p style="font-size: 14px;">${env.JOB_NAME}</p>
            </div>
            <div class="info-card">
                <h3>Build Status</h3>
                <p><span class="badge ${buildStatus == 'SUCCESS' ? 'badge-success' : 'badge-danger'}">${buildStatus}</span></p>
            </div>
        </div>
        
        <div class="section">
            <h2>🔧 Build Artifacts</h2>
            <table>
                <tr>
                    <th>Type</th>
                    <th>Details</th>
                    <th>Status</th>
                </tr>
                <tr>
                    <td><strong>Docker Images</strong></td>
                    <td>
                        <div style="margin: 3px 0;"><code style="background: #f1f1f1; padding: 2px 5px; border-radius: 3px;">${IMAGE_FULL}</code></div>
                        <div style="margin: 3px 0;"><code style="background: #f1f1f1; padding: 2px 5px; border-radius: 3px;">${IMAGE_VERSION}</code></div>
                        <div style="margin: 3px 0;"><code style="background: #f1f1f1; padding: 2px 5px; border-radius: 3px;">${IMAGE_LATEST}</code></div>
                    </td>
                    <td><span class="badge badge-success">Pushed</span></td>
                </tr>
                <tr>
                    <td><strong>Code Quality</strong></td>
                    <td>SonarQube Static Analysis</td>
                    <td><span class="badge badge-info">Completed</span></td>
                </tr>
                <tr>
                    <td><strong>Security Scan</strong></td>
                    <td>Trivy Vulnerability Scan</td>
                    <td>
                        ${env.TRIVY_TOTAL.toInteger() > 0 ? 
                            '<span class="badge badge-warning">' + env.TRIVY_TOTAL + ' vulnerabilities</span>' : 
                            '<span class="badge badge-success">Clean</span>'
                        }
                    </td>
                </tr>
            </table>
        </div>
        
        <div class="section">
            <h2>🛡️ Security Scan Results</h2>
            <div class="security-box ${env.TRIVY_TOTAL.toInteger() == 0 ? 'security-clean' : (env.TRIVY_CRITICAL.toInteger() > 0 ? 'security-danger' : 'security-warning')}">
                <div class="vulnerability-count">${TRIVY_TOTAL} Vulnerabilities Found</div>
                <p><strong>CRITICAL:</strong> <span class="badge ${env.TRIVY_CRITICAL.toInteger() > 0 ? 'badge-danger' : 'badge-success'}">${TRIVY_CRITICAL}</span></p>
                <p><strong>HIGH:</strong> <span class="badge ${env.TRIVY_HIGH.toInteger() > 0 ? 'badge-warning' : 'badge-success'}">${TRIVY_HIGH}</span></p>
                <p><em>Note: Build continues despite vulnerabilities based on current security policy.</em></p>
            </div>
        </div>
        
        <div class="section">
            <h2>🚀 Deployment Commands</h2>
            <div class="command-box">
                <pre style="margin: 0;"># Production Deployment (stable version)
docker pull ${IMAGE_VERSION}
docker run -d -p 8080:8080 --name todo-app-${SHORT_VERSION_TAG} ${IMAGE_VERSION}

# Testing Environment (specific build)
docker pull ${IMAGE_FULL}
docker run -d -p 8080:8080 --name todo-app-build-${BUILD_NUMBER} ${IMAGE_FULL}

# Development (latest build)
docker pull ${IMAGE_LATEST}
docker run -d -p 8080:8080 --name todo-app-latest ${IMAGE_LATEST}</pre>
            </div>
        </div>
        
        <div class="section">
            <h2>🔗 Quick Links</h2>
            <div class="links">
                <a href="${env.BUILD_URL}" target="_blank">Jenkins Build Details</a>
                <a href="${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}" target="_blank">SonarQube Report</a>
                <a href="https://hub.docker.com/r/${DOCKER_HUB_USER}/${IMAGE_NAME}/tags" target="_blank">Docker Hub</a>
            </div>
        </div>
        
        <div class="footer">
            <p>This is an automated message from Jenkins CI Pipeline.</p>
            <p>Jenkins URL: ${env.JENKINS_URL ?: 'Not configured'}</p>
            <p>© ${new Date().format('yyyy')} Spring Boot Todo App Team | Sent at: ${new Date().format("yyyy-MM-dd HH:mm:ss z")}</p>
        </div>
    </div>
</body>
</html>
                        """,
                        to: "${EMAIL_RECIPIENTS}",
                        from: "${EMAIL_FROM}",
                        replyTo: "${EMAIL_FROM}",
                        mimeType: "text/html"
                    )
                    
                    echo "Email notification sent successfully to ${EMAIL_RECIPIENTS}"
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
            echo "   Found ${TRIVY_TOTAL} vulnerabilities (${TRIVY_CRITICAL} CRITICAL, ${TRIVY_HIGH} HIGH)"
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
            echo "   2. SonarQube quality gate failed"
            echo "   3. Docker build/push failed"
            echo "   4. Trivy found HIGH or CRITICAL vulnerabilities in the Docker image"
            echo ""
            echo "Check the build logs for details."
        }
    }
}
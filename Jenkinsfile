pipeline {
    agent any

    tools {
        // Configure these tool names in Jenkins -> Manage Jenkins -> Tools
        jdk 'JDK-21'
        maven 'Maven-3.9'
        nodejs 'NodeJS-20'
    }

    environment {
        APP_NAME          = 'fintech-banking'
        BACKEND_IMAGE     = 'fintech-core-banking'
        FRONTEND_IMAGE    = 'fintech-banking-portal'
        IMAGE_TAG         = "${BUILD_NUMBER}"
        SONARQUBE_SERVER  = 'SonarQube' // Name configured in Jenkins -> Manage Jenkins -> System -> SonarQube servers
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }

    stages {
        // =========================================================================
        // Stage 1: Checkout Source Code
        // =========================================================================
        stage('Checkout SCM') {
            steps {
                echo 'Checking out source code from Git repository...'
                checkout scm
            }
        }

        // =========================================================================
        // Stage 2: Backend Compile, Test & JaCoCo Coverage
        // =========================================================================
        stage('Backend Build & Test') {
            steps {
                echo 'Compiling Spring Boot backend and running unit & integration tests...'
                sh '''
                    mvn clean test jacoco:report --batch-mode
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        // =========================================================================
        // Stage 3: SonarQube Static Code Analysis
        // =========================================================================
        stage('SonarQube Code Analysis') {
            steps {
                echo 'Executing SonarQube Scanner for code quality, bugs, and security vulnerabilities...'
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=digital-banking-fintech-platform \
                          -Dsonar.projectName="Digital Banking FinTech Platform" \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                          --batch-mode
                    '''
                }
            }
        }

        // =========================================================================
        // Stage 4: SonarQube Quality Gate Check
        // =========================================================================
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    echo 'Waiting for SonarQube Quality Gate result...'
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // =========================================================================
        // Stage 5: Frontend Build & TypeCheck
        // =========================================================================
        stage('Frontend Build & Validate') {
            steps {
                echo 'Installing frontend dependencies and verifying React TypeScript build...'
                dir('frontend') {
                    sh '''
                        npm ci
                        npx tsc --noEmit
                        npm run build
                    '''
                }
            }
        }

        // =========================================================================
        // Stage 6: Docker Image Build
        // =========================================================================
        stage('Docker Build Images') {
            steps {
                echo "Building Docker container images (Tag: ${IMAGE_TAG} and latest)..."
                sh '''
                    # Build Backend Core image
                    docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest .

                    # Build Frontend Portal image
                    docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest ./frontend
                '''
            }
        }

        // =========================================================================
        // Stage 7: Deploy via Docker Compose (Staging / Production)
        // =========================================================================
        stage('Deploy via Docker Compose') {
            when {
                branch 'main'
            }
            steps {
                echo 'Deploying application stack via Docker Compose...'
                sh '''
                    # Ensure .env exists from template if not present
                    if [ ! -f .env ]; then
                        cp .env.example .env
                    fi

                    # Launch/Update all containers with zero-downtime recreation
                    docker compose up -d --remove-orphans
                '''
            }
        }

        // =========================================================================
        // Stage 8: Health Check & Smoke Test
        // =========================================================================
        stage('Smoke Test & Health Probe') {
            when {
                branch 'main'
            }
            steps {
                echo 'Probing Actuator Health endpoint...'
                sh '''
                    # Wait up to 60 seconds for Spring Boot Actuator to report UP
                    for i in $(seq 1 12); do
                        if curl -s http://localhost:8080/actuator/health | grep '"status":"UP"'; then
                            echo "Application health verified: UP"
                            exit 0
                        fi
                        echo "Waiting for backend initialization... ($i/12)"
                        sleep 5
                    done
                    echo "Application failed health check probe!"
                    exit 1
                '''
            }
        }
    }

    // =============================================================================
    // Post Actions & Notifications
    // =============================================================================
    post {
        success {
            echo "CI/CD Pipeline Completed Successfully for Build #${BUILD_NUMBER}!"
        }
        failure {
            echo "CI/CD Pipeline Failed at stage. Please inspect console logs."
        }
        always {
            cleanWs deleteDirs: true, notFailBuild: true
        }
    }
}


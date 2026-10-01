pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
    }

    environment {
        NOTIFICATION_EMAIL_RECIPIENT = 'yahyakhaldy2@gmail.com, ouchchatea@gmail.com'
        COMPOSE_PROJECT_NAME = 'nexus'
    }

    stages {
        stage('Checkout Source Code') {
            agent { label 'backend' }
            steps {
                checkout scm
                script {
                    env.CURRENT_COMMIT_SHORT_HASH = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()
                }
                // Save the checked-out code so later stages running on a
                // DIFFERENT agent (frontend-agent) can reuse it without
                // cloning the repository a second time.
                stash name: 'source-code', includes: '**'
                echo "${NOTIFICATION_EMAIL_RECIPIENT}"
            }
        }

        stage('Detect Which Services Changed') {
            agent { label 'backend' }
            steps {
                unstash 'source-code'
                script {
                    def commitToCompareAgainst = env.CHANGE_TARGET ? "origin/${env.CHANGE_TARGET}" : 'HEAD~1'
                    sh '''
                        echo "Current commit:"
                        git rev-parse HEAD

                        echo
                        echo "Script contents:"
                        cat scripts/detect-changed-services.sh
                    '''
                    def detectionScriptOutput = sh(
                        script: "chmod +x scripts/detect-changed-services.sh && ./scripts/detect-changed-services.sh ${commitToCompareAgainst} HEAD",
                        returnStdout: true
                    ).trim()
                    env.CHANGED_SERVICE_NAMES = detectionScriptOutput.replaceAll('\n', ',')
                    echo "Services changed in this commit: ${env.CHANGED_SERVICE_NAMES}"
                }
            }
        }

        stage('Build And Test') {
            parallel {
                stage('Backend Services') {
                    agent { label 'backend' }
                    when {
                        beforeAgent true
                        expression {
                            env.CHANGED_SERVICE_NAMES.contains('discovery') ||
                            env.CHANGED_SERVICE_NAMES.contains('gateway') ||
                            env.CHANGED_SERVICE_NAMES.contains('media') ||
                            env.CHANGED_SERVICE_NAMES.contains('product') ||
                            env.CHANGED_SERVICE_NAMES.contains('user') ||
                            env.CHANGED_SERVICE_NAMES.contains('orders')
                        }
                    }
                    environment {
                        NEXUS_URL = 'http://nexus:8081'
                    }
                    steps {
                        unstash 'source-code'
                        script {
                            def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')
                            def changedBackendServiceNames = allChangedServiceNames.findAll {
                                it == 'discovery' || it == 'gateway' || it == 'media' || it == 'product' || it == 'user' || it == 'orders'
                            }

                            withCredentials([usernamePassword(credentialsId: 'nexus-ci-credentials', usernameVariable: 'NEXUS_CI_USER', passwordVariable: 'NEXUS_CI_PASSWORD')]) {
                                changedBackendServiceNames.each { serviceName ->
                                    dir("Backend/${serviceName}") {
                                        sh 'mvn -s ../../settings.xml clean package -U'
                                        junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                                    }
                                }
                            }
                        }
                    }
                }

                stage('Frontend Application') {
                    agent { label 'frontend' }
                    when {
                        beforeAgent true
                        expression { env.CHANGED_SERVICE_NAMES.contains('marketplace-ui') }
                    }
                    steps {
                        unstash 'source-code'
                        dir('marketplace-ui') {
                            sh 'npm ci'
                            sh 'npm test -- --watch=false --no-progress --coverage --coverage-reporters=lcov'
                            sh 'npm run build -- --configuration production'
                        }
                    }
                }
            }
        }

        stage('Static Code Analysis') {
            when { expression { env.CHANGED_SERVICE_NAMES?.trim() } }

            parallel {
                stage('Backend SonarQube Analysis') {
                    agent { label 'backend' }
                    when {
                        beforeAgent true
                        expression {
                            env.CHANGED_SERVICE_NAMES.contains('discovery') ||
                            env.CHANGED_SERVICE_NAMES.contains('gateway') ||
                            env.CHANGED_SERVICE_NAMES.contains('media') ||
                            env.CHANGED_SERVICE_NAMES.contains('product') ||
                            env.CHANGED_SERVICE_NAMES.contains('user') ||
                            env.CHANGED_SERVICE_NAMES.contains('orders')
                        }
                    }
                    steps {
                        unstash 'source-code'
                        script {
                            def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')
                            def changedBackendServiceNames = allChangedServiceNames.findAll {
                                it == 'discovery' || it == 'gateway' || it == 'media' || it == 'product' || it == 'user' || it == 'orders'
                            }
                            withSonarQubeEnv('sonarqube-server') {
                                changedBackendServiceNames.each { serviceName ->
                                    dir("Backend/${serviceName}") {
                                        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                            sh """

                                                   echo "Running SonarQube analysis for service: ${serviceName}"
                                                   mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                                                   -Dsonar.projectKey=buy01-${serviceName} \
                                                   -Dsonar.login=${SONAR_TOKEN}
                                            """
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                stage('Frontend SonarQube Analysis') {
                    agent { label 'frontend' }
                    when {
                        beforeAgent true
                        expression { env.CHANGED_SERVICE_NAMES.contains('marketplace-ui') }
                    }
                    steps {
                        unstash 'source-code'
                        withSonarQubeEnv('sonarqube-server') {
                            dir('marketplace-ui') {
                                sh 'sonar-scanner'
                            }
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            when { expression { env.CHANGED_SERVICE_NAMES?.trim() } }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Publish Backend Artifacts to Nexus') {
            agent { label 'backend' }
            when {
                beforeAgent true
                expression {
                    env.CHANGED_SERVICE_NAMES?.trim() && (
                        env.CHANGED_SERVICE_NAMES.contains('discovery') ||
                        env.CHANGED_SERVICE_NAMES.contains('gateway') ||
                        env.CHANGED_SERVICE_NAMES.contains('media') ||
                        env.CHANGED_SERVICE_NAMES.contains('product') ||
                        env.CHANGED_SERVICE_NAMES.contains('user') ||
                        env.CHANGED_SERVICE_NAMES.contains('orders')
                    )
                }
            }
            environment {
                NEXUS_URL = 'http://nexus:8081'
            }
            steps {
                unstash 'source-code'
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')
                    def changedBackendServiceNames = allChangedServiceNames.findAll {
                        it == 'discovery' || it == 'gateway' || it == 'media' || it == 'product' || it == 'user' || it == 'orders'
                    }
                    withCredentials([usernamePassword(credentialsId: 'nexus-ci-credentials', usernameVariable: 'NEXUS_CI_USER', passwordVariable: 'NEXUS_CI_PASSWORD')]) {
                        changedBackendServiceNames.each { serviceName ->
                            dir("Backend/${serviceName}") {
                                sh """
                                    mvn -s ../../settings.xml org.codehaus.mojo:versions-maven-plugin:2.16.2:set \
                                        -DnewVersion=0.0.1-${env.CURRENT_COMMIT_SHORT_HASH}-SNAPSHOT -DgenerateBackupPoms=false
                                    mvn -s ../../settings.xml -DskipTests deploy
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Build Container Images') {
            agent { label 'backend' }
            when {
                beforeAgent true
                expression { env.CHANGED_SERVICE_NAMES?.trim() }
            }
            steps {
                unstash 'source-code'
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    withCredentials([usernamePassword(credentialsId: 'nexus-ci-credentials', usernameVariable: 'NEXUS_CI_USER', passwordVariable: 'NEXUS_CI_PASSWORD')]) {
                        allChangedServiceNames.each { serviceName ->
                            sh """
                                echo "=== Building ${serviceName} ==="
                                IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                                docker compose --profile infra -f docker-compose.yml -f docker-compose.infra.yml --env-file /home/jenkins/.env build ${serviceName}

                                echo "=== Logging into Nexus ==="
                                echo "\$NEXUS_CI_PASSWORD" | docker login localhost:8082 -u "\$NEXUS_CI_USER" --password-stdin

                                echo "=== Tagging and Pushing ==="
                                docker tag ${serviceName}:${env.CURRENT_COMMIT_SHORT_HASH} localhost:8082/${serviceName}:${env.CURRENT_COMMIT_SHORT_HASH}
                                docker push localhost:8082/${serviceName}:${env.CURRENT_COMMIT_SHORT_HASH}
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy To Main Environment') {
            agent { label 'backend' }
            when {
                beforeAgent true
                allOf {
                    branch 'main'
                // expression { env.CHANGED_SERVICE_NAMES?.trim() }
                }
            }

            steps {
                unstash 'source-code'
                sh 'cp /home/jenkins/.env .env'

                sh """
                    IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                    docker compose \
                      --profile infra \
                      -f docker-compose.yml \
                      -f docker-compose.infra.yml \
                      --env-file /home/jenkins/.env \
                      up -d --no-deps discovery gateway product user media orders marketplace-ui
                """
            }
        }

        stage('Cleanup Unused Container Images') {
            agent { label 'backend' }
            when {
                beforeAgent true
                branch 'main'
            }
            steps {
                sh 'docker image prune -af --filter "until=72h" || true'
            }
        }
    }

    post {
        success {
                mail(
                    to: "${env.NOTIFICATION_EMAIL_RECIPIENT}",
                    subject: "SUCCESS: ${env.JOB_NAME} build #${env.BUILD_NUMBER} on branch ${env.BRANCH_NAME}",
                    body: "Services affected: ${env.CHANGED_SERVICE_NAMES ?: 'none'}\n\nFull build log: ${env.BUILD_URL}"
                )
        }

        failure {
            mail(
                to: "${NOTIFICATION_EMAIL_RECIPIENT}",
                subject: "FAILED: ${env.JOB_NAME} build #${env.BUILD_NUMBER} on branch ${env.BRANCH_NAME}",
                body: "Check the console output for details: ${env.BUILD_URL}console"
            )
        }
    }
}

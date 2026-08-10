pipeline {
    agent none

    environment {
        NOTIFICATION_EMAIL_RECIPIENT =
            'yahyakhaldy2@gmail.com, ouchchatea@gmail.com'

        COMPOSE_PROJECT_NAME = 'buy-01'
    }

    stages {

        stage('Checkout Source Code') {
            agent { label 'backend' }

            steps {
                checkout scm

                script {
                    env.CURRENT_COMMIT_SHORT_HASH =
                        env.GIT_COMMIT.take(7)
                }

                stash(
                    name: 'source-code',
                    includes: '**'
                )

                echo "${NOTIFICATION_EMAIL_RECIPIENT}"
            }
        }

        stage('Detect Which Services Changed') {
            agent { label 'backend' }

            steps {
                deleteDir()
                unstash 'source-code'

                script {
                    def commitToCompareAgainst =
                        env.CHANGE_TARGET
                            ? "origin/${env.CHANGE_TARGET}"
                            : 'HEAD~1'

                    def detectionScriptOutput = sh(
                        script: """
                            chmod +x scripts/detect-changed-services.sh

                            ./scripts/detect-changed-services.sh \
                                ${commitToCompareAgainst} \
                                HEAD
                        """,
                        returnStdout: true
                    ).trim()

                    env.CHANGED_SERVICE_NAMES =
                        detectionScriptOutput
                            .split('\n')
                            .collect { it.trim() }
                            .findAll { it }
                            .join(',')

                    echo "Changed services: ${env.CHANGED_SERVICE_NAMES ?: 'none'}"
                }
            }
        }

        stage('Build And Test') {

            parallel {

                stage('Backend Services') {
                    agent { label 'backend' }

                    when {
                        expression {
                            def changed =
                                env.CHANGED_SERVICE_NAMES ?: ''

                            changed.contains('discovery') ||
                            changed.contains('gateway') ||
                            changed.contains('media') ||
                            changed.contains('product') ||
                            changed.contains('user')
                        }
                    }

                    steps {
                        deleteDir()
                        unstash 'source-code'

                        script {
                            def backendServices = [
                                'discovery',
                                'gateway',
                                'media',
                                'product',
                                'user'
                            ]

                            def changedServices =
                                (env.CHANGED_SERVICE_NAMES ?: '')
                                    .split(',')
                                    .findAll { it }

                            changedServices
                                .findAll {
                                    backendServices.contains(it)
                                }
                                .each { serviceName ->

                                    dir("Backend/${serviceName}") {

                                        // 'verify' (not 'package') so the JaCoCo report
                                        // execution — bound to its default 'verify' phase —
                                        // actually runs and target/site/jacoco/jacoco.xml
                                        // exists before it gets stashed below.
                                        sh 'mvn clean verify'

                                        junit(
                                            testResults:
                                                'target/surefire-reports/*.xml'
                                        )

                                        stash(
                                            name: "backend-target-${serviceName}",
                                            includes: 'target/**'
                                        )
                                    }
                                }
                        }
                    }
                }

                stage('Frontend Application') {
                    agent { label 'frontend' }

                    when {
                        expression {
                            (env.CHANGED_SERVICE_NAMES ?: '')
                                .contains('marketplace-ui')
                        }
                    }

                    steps {
                        deleteDir()
                        unstash 'source-code'

                        dir('marketplace-ui') {
                            sh 'npm ci'

                            sh '''
                                npm test \
                                  -- --no-watch --no-progress
                            '''

                            stash(
                                name: 'frontend-coverage',
                                includes: 'coverage/**'
                            )

                            sh '''
                                npm run build \
                                  -- --configuration production
                            '''
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {

            parallel {

                stage('Backend SonarQube Analysis') {
                    agent { label 'backend' }

                    when {
                        expression {
                            def changed =
                                env.CHANGED_SERVICE_NAMES ?: ''

                            changed.contains('discovery') ||
                            changed.contains('gateway') ||
                            changed.contains('media') ||
                            changed.contains('product') ||
                            changed.contains('user')
                        }
                    }

                    steps {
                        deleteDir()
                        unstash 'source-code'

                        script {
                            def backendServices = [
                                'discovery',
                                'gateway',
                                'media',
                                'product',
                                'user'
                            ]

                            def changedServices =
                                (env.CHANGED_SERVICE_NAMES ?: '')
                                    .split(',')
                                    .findAll { it }

                            changedServices
                                .findAll {
                                    backendServices.contains(it)
                                }
                                .each { serviceName ->

                                    dir("Backend/${serviceName}") {

                                        unstash "backend-target-${serviceName}"

                                        withSonarQubeEnv(
                                            'My SonarQube Server'
                                        ) {

                                            sh """
                                                mvn sonar:sonar \
                                                  -Dsonar.projectKey=buy01-${serviceName} \
                                                  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                                            """
                                        }

                                        timeout(
                                            time: 10,
                                            unit: 'MINUTES'
                                        ) {
                                            waitForQualityGate(
                                                abortPipeline: true,
                                                webhookSecretId:
                                                    'sonarqube-webhook-secret'
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }

                stage('Frontend SonarQube Analysis') {
                    agent { label 'frontend' }

                    when {
                        expression {
                            (env.CHANGED_SERVICE_NAMES ?: '')
                                .contains('marketplace-ui')
                        }
                    }

                    steps {
                        deleteDir()
                        unstash 'source-code'

                        dir('marketplace-ui') {

                            unstash 'frontend-coverage'

                            withSonarQubeEnv(
                                'My SonarQube Server'
                            ) {

                                sh '''
                                    npx --yes @sonar/scan@4.3.8 \
                                      -Dsonar.host.url="$SONAR_HOST_URL" \
                                      -Dsonar.token="$SONAR_AUTH_TOKEN" \
                                      -Dsonar.projectKey=buy01-frontend
                                '''
                            }

                            timeout(
                                time: 10,
                                unit: 'MINUTES'
                            ) {
                                waitForQualityGate(
                                    abortPipeline: true,
                                    webhookSecretId:
                                        'sonarqube-webhook-secret'
                                )
                            }
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            agent { label 'backend' }

            when {
                expression {
                    (env.CHANGED_SERVICE_NAMES ?: '').trim()
                }
            }

            steps {
                deleteDir()
                unstash 'source-code'

                script {
                    def changedServices =
                        (env.CHANGED_SERVICE_NAMES ?: '')
                            .split(',')
                            .collect { it.trim() }
                            .findAll { it }

                    changedServices.each { serviceName ->

                        sh """
                            IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                            docker compose \
                              --profile infra \
                              -f docker-compose.yml \
                              -f docker-compose.jenkins.yml \
                              --env-file /home/jenkins/.env \
                              build ${serviceName}
                        """
                    }
                }
            }
        }

        stage('Deploy To Main Environment') {
            agent { label 'backend' }

            when {
                allOf {
                    branch 'main'
                    expression { (env.CHANGED_SERVICE_NAMES ?: '').trim() }
                }
            }

            steps {
                deleteDir()
                unstash 'source-code'

                sh 'cp /home/jenkins/.env .env'

                script {
                    def changedServices =
                        (env.CHANGED_SERVICE_NAMES ?: '')
                            .split(',')
                            .collect { it.trim() }
                            .findAll { it }
                            .join(' ')

                    sh """
                        IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                        docker compose \
                          --profile infra \
                          -f docker-compose.yml \
                          -f docker-compose.jenkins.yml \
                          --env-file /home/jenkins/.env \
                          up -d --no-deps \
                          ${changedServices}
                    """
                }
            }
        }
    }

    post {

        success {
            mail(
                to: "${env.NOTIFICATION_EMAIL_RECIPIENT}",
                subject:
                    "SUCCESS: ${env.JOB_NAME} build #${env.BUILD_NUMBER} " +
                    "on branch ${env.BRANCH_NAME}",
                body:
                    "Services affected: " +
                    "${env.CHANGED_SERVICE_NAMES ?: 'none'}\n\n" +
                    "Full build log: ${env.BUILD_URL}"
            )
        }

        failure {
            mail(
                to: "${env.NOTIFICATION_EMAIL_RECIPIENT}",
                subject:
                    "FAILED: ${env.JOB_NAME} build #${env.BUILD_NUMBER} " +
                    "on branch ${env.BRANCH_NAME}",
                body:
                    "Check the console output:\n" +
                    "${env.BUILD_URL}console"
            )
        }
    }
}

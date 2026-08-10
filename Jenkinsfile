pipeline {
    agent none

    environment {
        NOTIFICATION_EMAIL_RECIPIENT = 'yahyakhaldy2@gmail.com, ouchchatea@gmail.com'
        COMPOSE_PROJECT_NAME = "buy-01"
    }

    stages {
        stage('Checkout Source Code') {
            agent { label 'backend' }
            steps {
                checkout scm
                script {
                    env.CURRENT_COMMIT_SHORT_HASH = env.GIT_COMMIT.take(7)
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
                        expression {
                            env.CHANGED_SERVICE_NAMES.contains('discovery') ||
                            env.CHANGED_SERVICE_NAMES.contains('gateway') ||
                            env.CHANGED_SERVICE_NAMES.contains('media') ||
                            env.CHANGED_SERVICE_NAMES.contains('product') ||
                            env.CHANGED_SERVICE_NAMES.contains('user')
                        }
                    }
                    steps {
                        unstash 'source-code'
                        script {
                            def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')
                            def changedBackendServiceNames = allChangedServiceNames.findAll {
                                it == 'discovery' || it == 'gateway' || it == 'media' || it == 'product' || it == 'user'
                            }

                            changedBackendServiceNames.each { serviceName ->
                                dir("Backend/${serviceName}") {
                                    sh 'mvn clean package'
                                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                                }
                            }
                        }
                    }
                }

                stage('Frontend Application') {
                    agent { label 'frontend' }
                    when {
                        expression { env.CHANGED_SERVICE_NAMES.contains('marketplace-ui') }
                    }
                    steps {
                        unstash 'source-code'
                        dir('marketplace-ui') {
                            sh 'npm ci'
                            sh 'npm test -- --watch=false --no-progress'
                            sh 'npm run build -- --configuration production'
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            agent { label 'backend' }
            when { expression { env.CHANGED_SERVICE_NAMES?.trim() } }
            steps {
                unstash 'source-code'
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        sh """
                            IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                            docker compose --profile infra -f docker-compose.yml -f docker-compose.jenkins.yml --env-file /home/jenkins/.env build ${serviceName}
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
                    // expression { env.CHANGED_SERVICE_NAMES?.trim() }
                }
            }
            
            steps {
                unstash 'source-code'
                sh 'cp /home/jenkins/.env .env'

                // sh '''
                //     mkdir -p ssl
                //     cp -r /home/jenkins/ssl/* ssl/ || true
                // '''
                // script {
                //     def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',').findAll { it.trim() }

                //     allChangedServiceNames.each { serviceName ->
                //         sh """
                //             IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                //             docker compose -f docker-compose.yml -f docker-compose.jenkins.yml --env-file /home/jenkins/.env up -d ${serviceName}
                //         """
                //     }
                // }
                sh """
                    IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                    docker compose \
                      --profile infra \
                      -f docker-compose.yml \
                      -f docker-compose.jenkins.yml \
                      --env-file /home/jenkins/.env \
                      up -d --no-deps discovery gateway product user media marketplace-ui
                """
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

pipeline {
    agent none

    environment {
        // Who receives build status emails. Edit this directly — it's the
        // one line to change if the team's contact address changes.
        NOTIFICATION_EMAIL_RECIPIENT = "${env.SMTP_USERNAME}"
    }

    stages {

        stage('Checkout Source Code') {
            agent { label 'backend' }
            steps {
                checkout scm
                script {
                    // Short git commit hash — used to tag Docker images so every build
                    // produces a uniquely identifiable image, e.g. order-service:a1b2c3d
                    CURRENT_COMMIT_SHORT_HASH = "${env.GIT_COMMIT.take(7)}"
                }
                // Save the checked-out code so later stages running on a
                // DIFFERENT agent (frontend-agent) can reuse it without
                // cloning the repository a second time.
                stash name: 'source-code', includes: '**'
            }
        }

        stage('Detect Which Services Changed') {
            agent { label 'backend' }
            steps {
                unstash 'source-code'
                script {
                    // For a pull request, compare against its target branch.
                    // Otherwise compare against the previous commit on this branch.
                    def commitToCompareAgainst = env.CHANGE_TARGET ? "origin/${env.CHANGE_TARGET}" : "HEAD~1"

                    def detectionScriptOutput = sh(
                        script: "chmod +x scripts/detect-changed-services.sh && ./scripts/detect-changed-services.sh ${commitToCompareAgainst} HEAD",
                        returnStdout: true
                    ).trim()

                    // Store the result as a comma-separated string so later
                    // stages — which may run on a different agent — can read
                    // it back from the environment.
                    env.CHANGED_SERVICE_NAMES = detectionScriptOutput.replaceAll("\n", ",")
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
                                dir(serviceName) {
                                    sh 'mvn clean package'
                                    junit 'target/surefire-reports/*.xml'
                                }
                            }
                        }
                    }
                }

                stage('Frontend Application') {
                    agent { label 'frontend' }
                    when {
                        expression { env.CHANGED_SERVICE_NAMES.contains('frontend') }
                    }
                    steps {
                        unstash 'source-code'
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'ng test --watch=false --browsers=ChromeHeadless'
                            sh 'ng build --configuration production'
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
                        sh "docker build -t ${serviceName}:${CURRENT_COMMIT_SHORT_HASH} ./${serviceName}"
                    }
                }
            }
        }

        // stage('Load Environment') {
        //     agent { label 'backend' }

        //     steps {
        //         withCredentials([
        //             file(
        //                 credentialsId: 'buy01-env',
        //                 variable: 'ENV_FILE'
        //             )
        //         ]) {
        //             sh '''
        //                 cp "$ENV_FILE" .env
        //             '''
        //         }
        //     }
        // }

        stage('Deploy To Main Environment') {
            agent { label 'backend' }
            when { branch 'main' }
            steps {
                // steps {
                //     withCredentials([
                //         file(
                //             credentialsId: 'buy01-env',
                //             variable: 'ENV_FILE'
                //         )
                //     ]) {
                //         sh '''
                //             cp "$ENV_FILE" .env
                //         '''
                //     }
                // }
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        // No -f flag needed: docker-compose.yml is the default
                        // file, and it only contains application services.
                        sh """
                            IMAGE_TAG=${CURRENT_COMMIT_SHORT_HASH} \
                            docker compose --env-file /home/jenkins/.env up -d ${serviceName}
                        """
                    }
                }
            }
        }

        stage('Check Deployed Services Are Healthy') {
            agent { label 'backend' }
            when { branch 'main' }
            steps {
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        def healthCheckExitCode = sh(
                            script: "curl -sf http://${serviceName}:9000/actuator/health",
                            returnStatus: true
                        )
                        if (healthCheckExitCode != 0) {
                            error("Health check failed for service: ${serviceName}")
                        }

                        // Only after confirming the new version is genuinely
                        // healthy do we mark this image as "known good" — the
                        // version we fall back to if a future deploy fails.
                        sh "docker tag ${serviceName}:${CURRENT_COMMIT_SHORT_HASH} ${serviceName}:previous-good"
                    }
                }
            }
        }
    }

    post {
        success {
            mail(
                to: "${NOTIFICATION_EMAIL_RECIPIENT}",
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
            script {
                def deploymentAlreadyHappenedOnThisRun = (env.BRANCH_NAME == 'main' && env.CHANGED_SERVICE_NAMES?.trim())
                if (deploymentAlreadyHappenedOnThisRun) {
                    echo "Rolling back to the last known-good image for each affected service..."
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        sh """IMAGE_TAG=previous-good docker compose --env-file /home/jenkins/.env up -d ${serviceName} || true"""
                    }
                }
            }
        }
    }
}
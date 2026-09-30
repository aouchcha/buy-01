// ---------------------------------------------------------------------------
// Helpers (defined outside the pipeline block so every stage can reuse them)
// ---------------------------------------------------------------------------

// Names of the changed services, as a list. Empty list when nothing changed.
def changedServices() {
    def names = env.CHANGED_SERVICE_NAMES?.trim()
    return names ? names.split(',').collect { it.trim() }.findAll { it } : []
}

// Only the Java/Maven services (everything except the Angular frontend).
def changedBackendServices() {
    def backendServices = ['discovery', 'gateway', 'media', 'product', 'user', 'orders']
    return changedServices().findAll { backendServices.contains(it) }
}

def frontendChanged() {
    return changedServices().contains('marketplace-ui')
}

// Runs a block with the Nexus CI credentials exported as environment variables.
// settings.xml reads NEXUS_CI_USER / NEXUS_CI_PASSWORD / NEXUS_URL from the environment.
def withNexusCredentials(Closure body) {
    withCredentials([usernamePassword(
        credentialsId: 'nexus-ci-credentials',
        usernameVariable: 'NEXUS_CI_USER',
        passwordVariable: 'NEXUS_CI_PASSWORD'
    )]) {
        body()
    }
}

pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
    }

    environment {
        NOTIFICATION_EMAIL_RECIPIENT = 'yahyakhaldy2@gmail.com, ouchchatea@gmail.com'
        COMPOSE_PROJECT_NAME = "buy-02"

        // Needed by settings.xml (mirror URL) and by every pom's distributionManagement,
        // so it must be defined for ALL stages, not only the publish stage.
        NEXUS_URL = 'http://nexus:8081'

        // Base version of the services. The commit hash and -SNAPSHOT are added at publish time.
        BASE_VERSION = '0.0.1'
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
                        expression { changedBackendServices().size() > 0 }
                    }
                    steps {
                        unstash 'source-code'
                        script {
                            // Same settings.xml as the publish stage, so every stage resolves
                            // dependencies through Nexus and caches them under the same repo id.
                            withNexusCredentials {
                                changedBackendServices().each { serviceName ->
                                    dir("Backend/${serviceName}") {
                                        sh 'mvn -s ../../settings.xml clean package'
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
                        expression { frontendChanged() }
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
            when { expression { changedServices().size() > 0 } }

            parallel {
                stage('Backend SonarQube Analysis') {
                    agent { label 'backend' }
                    when {
                        beforeAgent true
                        expression { changedBackendServices().size() > 0 }
                    }
                    steps {
                        unstash 'source-code'
                        script {
                            withSonarQubeEnv('sonarqube-server') {
                                withNexusCredentials {
                                    changedBackendServices().each { serviceName ->
                                        dir("Backend/${serviceName}") {
                                            withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                                // \$SONAR_TOKEN is expanded by the shell (not Groovy),
                                                // so the secret is never interpolated into the script text.
                                                sh """
                                                    echo "Running SonarQube analysis for service: ${serviceName}"
                                                    mvn -s ../../settings.xml org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                                                        -Dsonar.projectKey=buy01-${serviceName} \
                                                        -Dsonar.login=\$SONAR_TOKEN
                                                """
                                            }
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
                        expression { frontendChanged() }
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
            when { expression { changedServices().size() > 0 } }
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
                expression { changedBackendServices().size() > 0 }
            }
            steps {
                unstash 'source-code'
                script {
                    // The version MUST end in -SNAPSHOT, otherwise Maven treats it as a release
                    // and deploys to <repository> (maven-releases) instead of <snapshotRepository>.
                    def publishVersion = "${env.BASE_VERSION}-${env.CURRENT_COMMIT_SHORT_HASH}-SNAPSHOT"

                    withNexusCredentials {
                        changedBackendServices().each { serviceName ->
                            dir("Backend/${serviceName}") {
                                sh """
                                    mvn -s ../../settings.xml org.codehaus.mojo:versions-maven-plugin:2.16.2:set \
                                        -DnewVersion=${publishVersion} -DgenerateBackupPoms=false
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
                expression { changedServices().size() > 0 }
            }
            steps {
                unstash 'source-code'
                script {
                    withNexusCredentials {
                        changedServices().each { serviceName ->
                            sh """
                                IMAGE_TAG=${env.CURRENT_COMMIT_SHORT_HASH} \
                                docker compose --profile infra -f docker-compose.yml -f docker-compose.infra.yml --env-file /home/jenkins/.env build ${serviceName}

                                echo "\$NEXUS_CI_PASSWORD" | docker login --tls-verify=false nexus:8082 -u "\$NEXUS_CI_USER" --password-stdin
                                docker tag ${serviceName}:${env.CURRENT_COMMIT_SHORT_HASH} nexus:8082/${serviceName}:${env.CURRENT_COMMIT_SHORT_HASH}
                                docker push --tls-verify=false nexus:8082/${serviceName}:${env.CURRENT_COMMIT_SHORT_HASH}
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
                    // Only deploy what was actually built. Unchanged services have no image
                    // tagged with this commit hash, so they must not be started with it.
                    expression { changedServices().size() > 0 }
                }
            }
            steps {
                unstash 'source-code'
                sh 'cp /home/jenkins/.env .env'

                script {
                    
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
                to: "${env.NOTIFICATION_EMAIL_RECIPIENT}",
                subject: "FAILED: ${env.JOB_NAME} build #${env.BUILD_NUMBER} on branch ${env.BRANCH_NAME}",
                body: "Check the console output for details: ${env.BUILD_URL}console"
            )
        }
    }
}
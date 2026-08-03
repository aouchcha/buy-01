FROM jenkins/jenkins:lts-jdk17

# Skip the interactive setup wizard — JCasC (below) configures everything it would have asked for
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG="/var/jenkins_home/casc_configs/jenkins.yaml"

USER root

# Install the Docker CLI so pipeline stages running here can run docker/docker compose commands
RUN apt-get update && apt-get install -y \
    ca-certificates curl gnupg lsb-release && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*

USER jenkins

# Install every plugin listed in plugins.txt at image-build time — no Plugin Manager UI needed
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Bake the JCasC config into the image as well
COPY jenkins.yaml /var/jenkins_home/casc_configs/jenkins.yaml
FROM jenkins/inbound-agent:latest-jdk21
 
USER root
RUN apt-get update && apt-get install -y \
    maven ca-certificates curl podman python3-yaml python3-dotenv && \
    curl -fsSL https://raw.githubusercontent.com/containers/podman-compose/v1.6.0/podman_compose.py \
      -o /usr/local/bin/podman-compose && \
    chmod 0755 /usr/local/bin/podman-compose && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /home/jenkins/.m2

ENV CONTAINER_HOST=unix:///var/run/podman/podman.sock \
    PODMAN_COMPOSE_PROVIDER=podman-compose
 
COPY --chown=jenkins:jenkins settings.xml /home/jenkins/.m2/settings.xml
 
RUN chown -R jenkins:jenkins /home/jenkins
 
USER jenkins
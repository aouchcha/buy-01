FROM jenkins/inbound-agent:latest-jdk21

USER root
RUN apt-get update && apt-get install -y curl ca-certificates && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g @angular/cli && \
    rm -rf /var/lib/apt/lists/* && \
    chown -R jenkins:jenkins /home/jenkins
RUN npm install -g sonar-scanner
    
USER jenkins
FROM jenkins/jenkins:lts-jdk21

# Skip the interactive setup wizard — JCasC (below) configures everything it would have asked for
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG="/var/jenkins_home/casc_configs/jenkins.yaml"

USER root

USER jenkins

# Install every plugin listed in plugins.txt at image-build time — no Plugin Manager UI needed
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Bake the JCasC config into the image as well
COPY jenkins.yaml /var/jenkins_home/casc_configs/jenkins.yaml
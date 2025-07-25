# Verwenden Sie das offizielle OpenJDK 17 Image als Basis
# FROM maven:3.8.5-openjdk-17
FROM maven:3.9.6-eclipse-temurin-21

# Setzen Sie das Arbeitsverzeichnis im Container
WORKDIR /app

COPY ./pom.xml ./pom.xml
COPY ./src ./src

# Install Kubernetes command line tool
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/

# Expose port 2605 where the server is addressed to
EXPOSE 2605

# # Compile Java project and create the JAR-file
RUN mvn package

COPY ./config.ini  ./config.ini

# Setzen des Einstiegspunkts, um die Main-Datei auszuführen
ENTRYPOINT ["java", "-jar", "target/DUUIRestService.jar"]
# CMD mvn exec:java -Dexec.mainClass="org.texttechnologylab.duui.api.Main"
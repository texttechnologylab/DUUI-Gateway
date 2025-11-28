# FROM maven:3.9.6-eclipse-temurin-21
FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean

# Optional: Version check
RUN java -version && mvn -version

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
RUN mvn package -DskipTests

COPY ./config.ini  ./config.ini

ENV JAVA_TOOL_OPTIONS="--add-opens=java.base/java.util=ALL-UNNAMED"

# Setzen des Einstiegspunkts, um die Main-Datei auszuführen
ENTRYPOINT ["java", "-jar", "target/DUUIRestService-1.0-all.jar"]
# CMD mvn exec:java -Dexec.mainClass="org.texttechnologylab.duui.api.Main"
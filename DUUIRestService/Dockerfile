# Verwenden Sie das offizielle OpenJDK 17 Image als Basis
FROM maven:3.8.4-openjdk-17

# Setzen Sie das Arbeitsverzeichnis im Container
WORKDIR /app

# Kopieren Sie den gesamten Projektordner in den Container
COPY . .

# Kompilieren der Java-Dateien
#RUN mkdir -p ./classes && \
#    javac -d ./classes $(find ./src/main/java -name "*.java")
RUN mvn package

# Erstellen Sie ein ausführbares JAR-File
#RUN jar cvfe ./target/DUUIRestService-1.0.jar org.texttechnologylab.duui.api.Main -C ./target/classes .

# Exponieren des Ports 2605 (falls benötigt)
EXPOSE 2605

# Setzen des Einstiegspunkts, um die JAR-Datei auszuführen
ENTRYPOINT ["java", "-jar", "./target/DUUIRestService.jar"]

# Übergeben der Konfigurationsdatei als Argument (passen Sie den Pfad an)
CMD ["./config.ini"]
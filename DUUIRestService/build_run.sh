export PORT=8085
export LOCAL_DRIVE_ROOT=./files
export HOST=localhost
export MONGO_HOST=localhost
export MONGO_DB=duui
export MONGO_PORT=27017
export MONGO_USER=db_user
export MONGO_PASSWORD=db_password
export DBX_APP_KEY=
export DBX_APP_SECRET=
export DBX_REDIRECT_URL=http://localhost:5173/account/dropbox
export GOOGLE_CLIENT_ID=
export GOOGLE_CLIENT_SECRET=
export GOOGLE_REDIRECT_URI=http://localhost:5173/account/google
export JAVA_TOOL_OPTIONS="--add-opens=java.base/java.util=ALL-UNNAMED"


# mvn clean compile
# mvn exec:java -Dexec.mainClass="org.texttechnologylab.duui.api.Main"
mvn clean -Dstyle.color=never -Djansi.strip=true package
#java -jar target/DUUIRestService-1.0-all.jar

#mvn -q -DskipTests package
#
## Confirm no signatures inside the fat jar
#jar tf target/*-all.jar | egrep 'META-INF/.*\.(SF|DSA|RSA|EC)' || echo "no signatures"
#
## (optional) confirm uimaFIT indices exist
#jar tf target/*-all.jar | grep 'META-INF/org.apache.uima.fit/'
#
java -jar target/DUUIRestService-1.0-all.jar
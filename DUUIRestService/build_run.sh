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
export SMPTP_HOST=localhost
export SMTP_PORT=1025
export SMTP_USER=
export SMTP_PASSWORD=
export MAIL_FROM_EMAIL=help@duui.de
export USE_SMTP_DEBUG=true

export JAVA_TOOL_OPTIONS="--add-opens=java.base/java.util=ALL-UNNAMED"

# To run a local SMTP server for testing email sending, you can use MailHog:
#  docker run --rm -p 1025:1025 -p 8025:8025 mailhog/mailhog

mvn clean compile
mvn  -Dstyle.color=never -Djansi.strip=true package
java -Djava.net.preferIPv4Stack=true -jar target/DUUIRestService-1.0-all.jar
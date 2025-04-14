export PORT=8085
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

mvn package
java -jar target/DUUIRestService.jar 
# Image bauen
docker build -t duui-rest-service .

# Container starten
docker run -v /var/run/docker.sock:/var/run/docker.sock --network host -p 2605:2605 duui-rest-service
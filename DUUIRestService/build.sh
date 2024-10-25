# Image bauen
docker build --no-cache -t docker.texttechnologylab.org/duui-rest-service .

# Container starten
#docker run -v /var/run/docker.sock:/var/run/docker.sock -v ~/.kube/config:/root/.kube/config --network host -p 2605:2605 duui-rest-service

# Build image
#docker build -t docker.texttechnologylab.org/duui-rest-service .

# Start container
#docker run -v /var/run/docker.sock:/var/run/docker.sock -v ~/.kube/config:/root/.kube/config -p 2605:2605 --rm --name duui-rest duui-rest-service

# Build and run with Docker-Compose
docker-compose up --build
FROM node:18-alpine AS builder

# URL auf der das Frontend läuft
ENV ORIGIN=https://duui.texttechnologylab.org

# URL auf der das Backend läuft
ENV API_URL=https://api.duui.texttechnologylab.org

RUN mkdir /app

COPY . /app

RUN cd /app && npm install && npm run build

FROM node:18-alpine


RUN mkdir /app
COPY --from=builder /app/build /app/build
COPY --from=builder /app/package.json /app/

RUN cd /app && \
    npm install --omit=dev

WORKDIR /app

EXPOSE 5173



# URL der Dropbox App
ENV DBX_URL='https://www.dropbox.com/home/Apps/Docker Unified UIMA Interface'

CMD [ "node", "build/index.js" ]
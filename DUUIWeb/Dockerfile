FROM node:18-alpine AS builder

ENV API_URL=http://duui-backend:2605

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

CMD ["node", "./build/index.js" ]
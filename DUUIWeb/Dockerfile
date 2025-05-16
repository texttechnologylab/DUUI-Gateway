FROM node:18-alpine AS builder

ARG API_URL
ENV API_URL=http://duui-gateway-backend:2605
ARG DBX_URL
ENV DBX_URL=${DBX_URL}

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
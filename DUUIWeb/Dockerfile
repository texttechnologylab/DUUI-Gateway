FROM node:18-alpine AS builder

ARG API_URL
ARG DBX_URL
ARG PUBLIC_VERIFICATION_REQUIRED

ENV API_URL=${API_URL}
ENV DBX_URL=${DBX_URL}
ENV PUBLIC_VERIFICATION_REQUIRED=${PUBLIC_VERIFICATION_REQUIRED}

RUN mkdir /app

COPY . /app

RUN cd /app && npm install && npm run build

FROM node:18-alpine

# Installiere Python sowie notwendige Build-Tools
RUN apk add --no-cache python3 make g++

RUN mkdir /app
COPY --from=builder /app/build /app/build
COPY --from=builder /app/package.json /app/

RUN cd /app && \
    npm install --omit=dev --ignore-scripts && \
    npm rebuild bcrypt --build-from-source

WORKDIR /app

EXPOSE 5173


CMD ["node", "./build/index.js" ]
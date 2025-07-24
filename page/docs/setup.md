# DUUI-Gateway Setup Guide

This document describes how to set up and configure DUUI-Gateway, including Docker-based deployment and necessary configuration.

---

## Overview

DUUI-Gateway consists of two main components:

- **Backend**: REST API for NLP processing
- **Frontend**: Web interface built with SvelteKit

Both components are containerized using Docker, simplifying setup and deployment.

---

## Prerequisites

Make sure you have installed:

- Docker and Docker Compose
- Node.js and npm (for frontend development)
- Java 21

---

## Docker Setup

The application uses Docker Compose for simple setup and deployment. Below is an explanation of the provided Docker Compose configuration:

### Docker Compose Explanation (`docker-compose.yml`)

The docker-compose file defines two services:

- **duui-backend**:
    - Builds from `./DUUIRestService`
    - Exposed on port `2605`
    - Mounts Docker socket (`/var/run/docker.sock`) allowing the backend to manage Docker containers itself.

- **duui-frontend**:
    - Builds from `./DUUIWeb`
    - Depends on the backend
    - Exposed on port `5173`
    - Environment variables configure frontend-backend connectivity (`API_URL=http://duui-backend:2605`)

### Example:

```yaml
version: '3.3'

services:
  duui-backend:
    build:
      context: ./DUUIRestService
      dockerfile: Dockerfile
    container_name: duui-backend
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    ports:
      - "2605:2605"

  duui-frontend:
    build:
      context: ./DUUIWeb
      dockerfile: Dockerfile
    container_name: web-frontend
    ports:
      - "5173:5173"
    depends_on:
      - duui-backend
    environment:
      - API_URL=http://duui-backend:2605
      - PORT=5173
      - DBX_URL='https://www.dropbox.com/home/Apps/DUUI'
```

---

## Configuration (`config.ini`)

Configuration variables are managed through `config.ini`, which contains API credentials, server port, file locations, and database connections.

### Example `config.ini`:

```ini
# Dropbox configuration
DBX_APP_KEY=
DBX_APP_SECRET=
DBX_REDIRECT_URL=

# Google Drive configuration
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=

# Application configuration
PORT=2605  # Match with Docker Compose backend port
HOST=0.0.0.0  # 0.0.0.0 makes backend accessible from all IP addresses

# File Upload Configuration
FILE_UPLOAD_DIRECTORY=/temp

# MongoDB Connection
MONGO_HOST=
MONGO_PORT=
MONGO_DB=
MONGO_USER=
MONGO_PASSWORD=

# Alternatively, provide a MongoDB connection string
MONGO_DB_CONNECTION_STRING=
```

---

## Obtaining API Keys

You must obtain keys for Dropbox and Google Drive:

- **Dropbox**: [Dropbox Developers Console](https://www.dropbox.com/developers)
- **Google Drive**: [Google Cloud Console](https://console.cloud.google.com/apis/dashboard)

Follow their respective documentation to set up OAuth credentials.

---

## Running the Application

To deploy locally:

1. Clone repository and navigate into the root directory.
2. Start the application using Docker Compose:

```bash
docker-compose up --build
```

The frontend will be available at [http://localhost:5173](http://localhost:5173).

To ensure the frontend correctly communicates with the backend, ensure ports in `config.ini` and `docker-compose.yml` match appropriately (`PORT=2605`).

### Frontend Development Setup

For local frontend development:

```bash
cd DUUIWeb
npm install
npm run dev -- --open --host
```

This setup provides auto-reload functionality, ideal for iterative frontend development.

---

## Security and Access Control

DUUI-Gateway adheres to industry-standard security practices for handling API keys, secrets, and authentication. However, the application is actively developed, and additional security measures are continuously being implemented. Developers and deployers should remain cautious, keep security best practices in mind, and regularly update the system to mitigate any emerging security concerns.

---

## Further Documentation

Refer to the [DUUI-Gateway Documentation](https://texttechnologylab.github.io/DUUI-Gateway/) for more detailed instructions, API references, and developer guidelines.

---

This comprehensive setup guide helps developers get started quickly and contributes effectively to ongoing development.


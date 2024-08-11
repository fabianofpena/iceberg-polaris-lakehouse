# Polaris Project Repository

This repository contains the Polaris project, including various modules and deployment configurations for running the Polaris application on Kubernetes.

## Table of Contents

- [Project Overview](#project-overview)
- [Repository Structure](#repository-structure)
- [Setup and Installation](#setup-and-installation)
- [Usage](#usage)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## Project Overview

The Polaris project is designed to manage and interact with Polaris catalogs and principals through a robust API client. It provides functionality for creating catalogs, principals, roles, and managing their relationships and permissions in a scalable environment.

### Directory Details

- **`deployment/`**: Contains Helm charts and Kubernetes configuration files for deploying the Polaris application. [Read more](deployment/readme.md).
- **`polaris/`**: Contains the main application entry point and associated modules.
- **`polaris_client/`**: Contains the Polaris API client logic, including entities, interfaces, repositories, and use cases.
- **`spark/`**: Includes Spark jobs and related configuration files for processing data within the Polaris ecosystem.

## Setup and Installation

### Prerequisites

- Python 3.8+
- Docker (for containerization and deployment)
- Kubernetes (for running the application in a cluster)
- Helm 3.x (for Kubernetes deployments)
- Apache Spark (if using Spark jobs)

## Usage

### Running Polaris with Docker Compose

You can easily deploy Polaris using Docker Compose. This setup is particularly useful for quickly spinning up the Polaris service with all necessary configurations.

#### Prerequisites

Before running the Docker Compose setup, you need to configure your AWS credentials and region.

1. **Rename the `.env.example` file**:

In the root of your project, you should find a file named `.env.example`. Rename this file to `.env`:

```bash
mv .env.example .env
```

2. **Edit the the `.env` file**:

Open the .env file in your favorite text editor and replace the placeholder values with your actual AWS credentials and region:

```bash
AWS_ACCESS_KEY_ID=<your-aws-access-key-id>
AWS_SECRET_ACCESS_KEY=<your-aws-secret-access-key>
AWS_REGION=<your-aws-region>
```

3. **Start Polaris**:

```bash
make up
```

This command will build and start the Polaris container in detached mode, exposing the service on ports 8181 and 8182.

4. **View Logs and Retrieve Root Keys**:

To view the logs of the running Polaris container and retrieve the pair of root keys, run:


```bash
make logs
```

In the logs, search for the following line to find your root principal credentials:

```bash
polaris-service  | realm: default-realm root principal credentials: <client_id>:<client_secret>
```

Once you have retrieved the root principal credentials, you can use them to run the polaris_client app. This client allows you to create catalogs, principals, roles, and manage permissions within the Polaris service.

Replace the example client_id and client_secret with the ones you obtained from the logs, and run the following command:

```bash
python run.py \
    --client_id <your_client_id> \
    --client_secret <your_client_secret> \
    --host http://localhost:8181/api/catalog \
    --catalog_name <your_catalog_name> \
    --s3_location <your_s3_location> \
    --role_arn <your_aws_role_arn> \
    --principal_name <your_principal_name> \
    --principal_role_name <your_principal_role_name> \
    --catalog_role_name <your_catalog_role_name> \
    --role_type <role_type>
```


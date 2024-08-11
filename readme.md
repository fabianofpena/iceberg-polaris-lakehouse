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



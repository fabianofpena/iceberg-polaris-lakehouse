# Polaris Client

This directory contains the Polaris API client, which is designed to interact with Polaris catalogs, principals, and roles within a data lake environment. The client follows Clean Code Architecture principles and is built to be modular, maintainable, and scalable.

## Table of Contents

- [Project Structure](#project-structure)
- [Setup and Installation](#setup-and-installation)
- [Usage](#usage)
- [Docker](#docker)
- [Contributing](#contributing)
- [License](#license)

## Project Structure

The `polaris_client` directory is organized as follows:

```plaintext
.
├── Dockerfile             # Dockerfile for containerizing the Polaris client
├── core                   # Core domain entities and interfaces
├── infra                  # Infrastructure-related code, including repositories and configurations
├── interface              # Interface layer, including CLI argument parsing
├── main                   # Main application entry point and controllers
├── run.py                 # Entry point to run the Polaris client
└── use_cases              # Application use cases
├── README.md              # This README file
```

## Local environment
```sh
# Setup virtual environment
cd polaris/regtests/client/python
python3 -m venv .venv
source .venv/bin/activate

# Install Polaris Dependencies
pip install poetry==1.5.0
poetry install && pip install -e .

# Install Pylint and Pre-Commit
cd ../../../../polaris_client
pip3 install pylint
pylint --generate-rcfile > .pylintrc
pip3 install pre-commit
pre-commit install
``` 

## Directory Details

- **`Dockerfile`**: A Dockerfile for containerizing the Polaris client, making it easier to deploy and run in various environments.
- **`architecture/`**: Contains architecture-related documentation, diagrams, and explanations about the design and structure of the Polaris client.
- **`core/`**: Contains core domain entities and interfaces, such as the Principal, Catalog, Role, and their related interfaces. This is the heart of the business logic and follows SOLID principles.
- **`export_scripts`**.sh: Shell script for exporting environment variables or other configurations needed to run the Polaris client.
- **`infra/`**: Infrastructure-related code, including repositories that implement the interfaces defined in core/. This layer interacts with external systems like Polaris API, AWS S3, and databases.
- **`interface/`**: Handles user input and output, including command-line interface (CLI) parsing using argparse. This is where the arguments for the client are processed.
- **`main/`**: The entry point of the application, responsible for orchestrating the use cases and interacting with the infrastructure layer. It includes controllers that manage the flow of data through the application.
- **`run`**.py: The main script that runs the Polaris client. This script parses command-line arguments and executes the necessary use cases based on the input.
- **`use_cases/`**: Contains the application-specific use cases that orchestrate the flow of information between the core/ and infra/ layers. Each use case corresponds to a specific function of the Polaris client, such as creating a catalog, managing roles, or assigning privileges.

## Usage
To run the Polaris client and interact with the Polaris API, use the run.py script. Here’s an example command:
```sh
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

## Arguments

- **`--client_id`**: The unique identifier for the principal making the API request.
- **`--client_secret`**: The secret key associated with the client_id.
- **`--host`**: The base URL of the Polaris API.
- **`--catalog_name`**: The name of the catalog you wish to create or manage.
- **`--s3_location`**: The S3 bucket location where the catalog data will be stored.
- **`--role_arn`**: The ARN of the IAM role with permissions to access the S3 bucket.
- **`--principal_name`**: The name of the principal (user or service) to be created or managed.
- **`--principal_role_name`**: The name of the role to assign to the principal.
- **`--catalog_role_name`**: The name of the role to assign within the catalog.
- **`--role_type`**: Specifies the type of role (admin, reader, etc.) for access control.

## Contributing
Contributions to the Polaris client are welcome! Please fork the repository, make your changes, and submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

## Creating an External Catalog in StarRocks

To integrate StarRocks with Polaris, you can create an external catalog that allows StarRocks to interact with Iceberg tables managed by Polaris. Below are the steps to create this external catalog.

### Step 1: Create the External Catalog

You can create an external catalog in StarRocks using the following SQL command. This command connects to the Polaris service and defines the Iceberg catalog that StarRocks will use.

```sql
CREATE EXTERNAL CATALOG <your_catalog_role_name>
PROPERTIES
(
    "type" = "iceberg",
    "iceberg.catalog.type" = "rest",
    "iceberg.catalog.uri" = "http://polaris.metastore.svc.cluster.local:8181/api/catalog",
    "iceberg.catalog.credential" = "<client_id>:<client_secret>",
    "iceberg.catalog.scope" = "PRINCIPAL_ROLE:ALL",
    "iceberg.catalog.warehouse" = "<your_catalog_role_name>"
);
SET catalog polaris_catalog;
SHOW DATABASES;
USE bronze;
SHOW TABLES;
```

- **`type`**: Specifies the catalog type, in this case, iceberg.
- **`iceberg.catalog.type`**: Specifies the type of Iceberg catalog, set to rest for Polaris.
- **`iceberg.catalog.uri`**: The URI of the Polaris Iceberg catalog REST API.
- **`iceberg.catalog.credential`**: The credentials (client ID and client secret) retrieved from the Polaris service logs.
- **`iceberg.catalog.scope`**: The scope for the Iceberg catalog, typically set to PRINCIPAL_ROLE:ALL.
- **`iceberg.catalog.warehouse`**: Specifies the warehouse location in Polaris that corresponds to the catalog.

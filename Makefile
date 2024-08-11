# Define environment variables
include .env

# Default target
.PHONY: all
all: up

# Target to bring up the Polaris service
.PHONY: up
up:
	docker-compose up -d

# Target to stop the Polaris service
.PHONY: down
down:
	docker-compose down

# Target to view logs
.PHONY: logs
logs:
	docker-compose logs -f

# Target to rebuild and bring up the Polaris service
.PHONY: rebuild
rebuild:
	docker-compose down
	docker-compose up --build -d

# Target to clean up and remove any stopped containers, networks, volumes, and images
.PHONY: clean
clean:
	docker-compose down --rmi all --volumes --remove-orphans

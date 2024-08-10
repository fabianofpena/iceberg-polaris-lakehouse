# General AWS settings
AWS_REGION_DEV := us-east-2
AWS_REGION_PRD := us-east-1
AWS_ACCOUNT_ID_DEV := 264426555227
AWS_ACCOUNT_ID_PRD := 621936395696
IMAGE_NAME := polaris-polaris
IMAGE_TAG := 1.0.0

# User-specific AWS settings
USER_DEV := fabianop
USER_PRD := fabianop-prod

# Default action
.PHONY: all
all: dev

# Environment-specific actions
.PHONY: dev prd
dev prd:
	@$(MAKE) deploy ENV=$@

# Determine the right AWS account ID and region based on the environment
deploy:
	$(eval AWS_ACCOUNT_ID := $(AWS_ACCOUNT_ID_$(shell echo $(ENV) | tr a-z A-Z)))
	$(eval AWS_REGION := $(AWS_REGION_$(shell echo $(ENV) | tr a-z A-Z)))
	$(eval USER := $(USER_$(shell echo $(ENV) | tr a-z A-Z)))
	$(eval REGISTRY := $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com)
	$(eval IMAGE_FULLNAME := $(REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG))
	@echo Deploying to $(ENV) environment...
	@aws ecr get-login-password --region $(AWS_REGION) --profile $(USER) | docker login --username AWS --password-stdin $(REGISTRY)
	@docker build . -t $(IMAGE_NAME):$(IMAGE_TAG) --platform linux/amd64
	@docker tag $(IMAGE_NAME):$(IMAGE_TAG) $(IMAGE_FULLNAME)
	@docker push $(IMAGE_FULLNAME)
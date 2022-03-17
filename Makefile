IMAGE_PREFIX = docker.causeex.com/dart/
IMAGE_NAME = dart-es
IMG := $(IMAGE_PREFIX)$(IMAGE_NAME)

ifndef CI_COMMIT_REF_NAME
	APP_VERSION := "latest"
else ifeq ("$(CI_COMMIT_REF_NAME)", "master")
	APP_VERSION := "latest"
else ifdef CI_COMMIT_TAG
	APP_VERSION := $(shell cat version.sbt | cut -d\" -f2 | cut -d '-' -f1)
else
	APP_VERSION := $(CI_COMMIT_REF_NAME)
endif

docker-build:
	./es/download.sh
	docker build --pull -t $(IMG):$(APP_VERSION) .

docker-push: docker-build
	docker push $(IMG):$(APP_VERSION)

IMAGE_PREFIX = ""
IMAGE_NAME = dart-es
IMG := $(IMAGE_PREFIX)$(IMAGE_NAME)

docker-build:
	./es/download.sh
	docker build --pull -t $(IMG):$(APP_VERSION) .

docker-push: docker-build
	docker push $(IMG):$(APP_VERSION)

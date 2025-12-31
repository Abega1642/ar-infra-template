.PHONY: help build test format format-check clean docker-build docker-run docker-stop \
        deps compile jar install run dev ci-build ci-test ci-format \
        qodana security-scan verify-image health-check lint test-unit test-integration

GRADLE := ./gradlew
DOCKER := docker
IMAGE_NAME := backend-app
IMAGE_TAG := latest
CONTAINER_NAME := backend-app-container
PORT := 8080

.DEFAULT_GOAL := help

help:
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} \
		/^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2 } \
		/^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

deps: ## Download and cache dependencies
	$(GRADLE) dependencies --no-daemon

compile: ## Compile the application
	$(GRADLE) compileJava --no-daemon

jar: ## Build JAR file
	$(GRADLE) bootJar --no-daemon

build: format-check compile jar ## Full build with format check

run: ## Run the application
	$(GRADLE) bootRun --no-daemon

dev: ## Run with development profile
	$(GRADLE) bootRun --no-daemon --args='--spring.profiles.active=dev'

##@ Testing

test: ## Run all tests
	@TESTCONTAINERS_RYUK_DISABLED=true \
	TESTCONTAINERS_CHECKS_DISABLE=true \
	$(GRADLE) clean test --info --no-daemon

test-unit: ## Run unit tests only
	$(GRADLE) test --tests '*Test' --no-daemon

test-integration: ## Run integration tests only
	@TESTCONTAINERS_RYUK_DISABLED=true \
	$(GRADLE) test --tests '*IT' --no-daemon

##@ Code Quality

format: ## Format code (Java and YAML)
	@chmod +x format.sh
	@./format.sh

format-check: ## Verify code formatting
	@chmod +x format.sh
	@./format.sh
	@git diff --exit-code || (echo "Formatting issues detected. Run 'make format' to fix." && exit 1)

lint: ## Run linting checks
	$(GRADLE) check --no-daemon

qodana: ## Run Qodana code analysis
	@docker run --rm -v $(PWD):/data/project jetbrains/qodana-jvm-community:latest

security-scan: ## Run Semgrep security scanning
	@command -v semgrep >/dev/null 2>&1 && semgrep --config=auto . || echo "Semgrep not installed"

##@ Docker

docker-build: ## Build Docker image
	$(DOCKER) build -t $(IMAGE_NAME):$(IMAGE_TAG) .

docker-build-cache: ## Build Docker image with buildx cache
	$(DOCKER) buildx build \
		--cache-from type=local,src=/tmp/.buildx-cache \
		--cache-to type=local,dest=/tmp/.buildx-cache \
		-t $(IMAGE_NAME):$(IMAGE_TAG) .

verify-image: ## Verify Docker image contents and configuration
	@echo "Verifying image exists:"
	@$(DOCKER) images | grep $(IMAGE_NAME)
	@echo "\nVerifying JAR file:"
	@$(DOCKER) run --rm --entrypoint sh $(IMAGE_NAME):$(IMAGE_TAG) -c "ls -lh /app/app.jar"
	@echo "\nVerifying Java version:"
	@$(DOCKER) run --rm --entrypoint sh $(IMAGE_NAME):$(IMAGE_TAG) -c "java -version"

docker-run: ## Run Docker container
	$(DOCKER) run -d \
		--name $(CONTAINER_NAME) \
		-p $(PORT):8080 \
		--env-file .env \
		$(IMAGE_NAME):$(IMAGE_TAG)

docker-stop: ## Stop and remove Docker container
	@$(DOCKER) stop $(CONTAINER_NAME) 2>/dev/null || true
	@$(DOCKER) rm $(CONTAINER_NAME) 2>/dev/null || true

docker-logs: ## View container logs
	$(DOCKER) logs -f $(CONTAINER_NAME)

health-check: ## Check application health endpoint
	@curl -f http://localhost:$(PORT)/actuator/health

##@ CI/CD

ci-build: docker-build verify-image ## Run CI build pipeline locally

ci-test: test ## Run CI test pipeline locally

ci-format: format-check ## Run CI format check locally

##@ Cleanup

clean: ## Clean build artifacts
	$(GRADLE) clean --no-daemon
	@rm -rf build/

clean-docker: docker-stop ## Remove Docker images and containers
	@$(DOCKER) rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true

clean-all: clean clean-docker ## Complete cleanup

##@ Setup

install: ## Install and verify development tools
	@test -f google-java-format-1.28.0-all-deps.jar || \
		curl -L -o google-java-format-1.28.0-all-deps.jar \
		https://github.com/google/google-java-format/releases/download/v1.28.0/google-java-format-1.28.0-all-deps.jar
	@chmod +x format.sh yamlfmt 2>/dev/null || true

verify: ## Verify development environment
	@java -version
	@$(GRADLE) --version
	@docker --version
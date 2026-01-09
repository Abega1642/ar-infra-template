.PHONY: help build test format format-check clean docker-build docker-run docker-stop \
        deps compile jar install run dev ci-build ci-test ci-format ci-qodana ci-semgrep \
        qodana semgrep verify-image health-check lint test-unit test-integration \
        install-pipx install-semgrep

DOCKER := docker
IMAGE_NAME := backend-app
IMAGE_TAG := latest
CONTAINER_NAME := backend-app-container
PORT := 8080

UNAME_S := $(shell uname -s 2>/dev/null || echo Windows)
ifeq ($(OS),Windows_NT)
	DETECTED_OS := Windows
else ifeq ($(UNAME_S),Linux)
	DETECTED_OS := Linux
else ifeq ($(UNAME_S),Darwin)
	DETECTED_OS := MacOS
else ifeq ($(findstring MINGW,$(UNAME_S)),MINGW)
	DETECTED_OS := Windows
else ifeq ($(findstring MSYS,$(UNAME_S)),MSYS)
	DETECTED_OS := Windows
else ifeq ($(findstring CYGWIN,$(UNAME_S)),CYGWIN)
	DETECTED_OS := Windows
else
	DETECTED_OS := Unknown
endif

# Set GRADLE command based on OS
ifeq ($(DETECTED_OS),Windows)
	GRADLE := gradlew.bat
	PWD := $(shell cd)
else
	GRADLE := ./gradlew
	PWD := $(shell pwd)
endif

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
ifeq ($(DETECTED_OS),Windows)
	@set TESTCONTAINERS_RYUK_DISABLED=true && set TESTCONTAINERS_CHECKS_DISABLE=true && $(GRADLE) clean test --info --no-daemon
else
	@TESTCONTAINERS_RYUK_DISABLED=true \
	TESTCONTAINERS_CHECKS_DISABLE=true \
	$(GRADLE) clean test --info --no-daemon
endif

test-unit: ## Run unit tests only
	$(GRADLE) test --tests '*Test' --no-daemon

test-integration: ## Run integration tests only
ifeq ($(DETECTED_OS),Windows)
	@set TESTCONTAINERS_RYUK_DISABLED=true && $(GRADLE) test --tests '*IT' --no-daemon
else
	@TESTCONTAINERS_RYUK_DISABLED=true \
	$(GRADLE) test --tests '*IT' --no-daemon
endif

##@ Code Quality

format: ## Format code (Java and YAML)
	@if [ -f "format.sh" ]; then \
		chmod +x format.sh && ./format.sh; \
	elif [ -f "format.bat" ]; then \
		cmd //c format.bat; \
	else \
		echo "Error: No format script found (format.sh or format.bat)"; \
		exit 1; \
	fi

format-check: ## Verify code formatting
	@if [ -f "format.sh" ]; then \
		chmod +x format.sh && ./format.sh; \
	elif [ -f "format.bat" ]; then \
		cmd //c format.bat; \
	else \
		echo "Error: No format script found (format.sh or format.bat)"; \
		exit 1; \
	fi
	@git diff --exit-code || (echo "Formatting issues detected. Run 'make format' to fix." && exit 1)

lint: ## Run linting checks
	$(GRADLE) check --no-daemon

qodana: ## Run Qodana code analysis
ifeq ($(DETECTED_OS),Windows)
	@if not exist qodana-results mkdir qodana-results
	@docker run --rm -v $(PWD):/data/project -v $(PWD)/qodana-results:/data/results jetbrains/qodana-jvm-community:latest --save-report --results-dir=/data/results
else
	@mkdir -p $(PWD)/qodana-results
	@docker run --rm \
		-v $(PWD):/data/project \
		-v $(PWD)/qodana-results:/data/results \
		jetbrains/qodana-jvm-community:latest \
		--save-report --results-dir=/data/results
endif

install-pipx: ## Install pipx based on OS
ifeq ($(DETECTED_OS),Linux)
	@if ! command -v pipx >/dev/null 2>&1; then \
		echo "Installing pipx on Linux..."; \
		if command -v apt-get >/dev/null 2>&1; then \
			sudo apt-get update && sudo apt-get install -y pipx; \
		elif command -v dnf >/dev/null 2>&1; then \
			sudo dnf install -y pipx; \
		elif command -v yum >/dev/null 2>&1; then \
			sudo yum install -y pipx; \
		elif command -v pacman >/dev/null 2>&1; then \
			sudo pacman -S --noconfirm python-pipx; \
		else \
			python3 -m pip install --user pipx; \
			python3 -m pipx ensurepath; \
		fi; \
	else \
		echo "pipx is already installed"; \
	fi
else ifeq ($(DETECTED_OS),MacOS)
	@if ! command -v pipx >/dev/null 2>&1; then \
		echo "Installing pipx on MacOS..."; \
		if command -v brew >/dev/null 2>&1; then \
			brew install pipx; \
			pipx ensurepath; \
		else \
			python3 -m pip install --user pipx; \
			python3 -m pipx ensurepath; \
		fi; \
	else \
		echo "pipx is already installed"; \
	fi
else ifeq ($(DETECTED_OS),Windows)
	@where pipx >nul 2>nul || ( \
		echo Installing pipx on Windows... && \
		python -m pip install --user pipx && \
		python -m pipx ensurepath \
	) || ( \
		python3 -m pip install --user pipx && \
		python3 -m pipx ensurepath \
	)
	@where pipx >nul 2>nul && echo pipx is already installed
else
	@echo "Unknown OS. Please install pipx manually."
	@exit 1
endif

install-semgrep: install-pipx ## Install Semgrep using pipx
ifeq ($(DETECTED_OS),Windows)
	@where semgrep >nul 2>nul || ( \
		echo Installing Semgrep via pipx... && \
		pipx install semgrep || (echo Failed to install Semgrep && exit 1) \
	)
	@where semgrep >nul 2>nul && echo Semgrep is already installed
else
	@if ! command -v semgrep >/dev/null 2>&1; then \
		echo "Installing Semgrep via pipx..."; \
		pipx install semgrep || (echo "Failed to install Semgrep" && exit 1); \
	else \
		echo "Semgrep is already installed"; \
	fi
endif

semgrep: install-semgrep ## Run Semgrep security scanning
	@semgrep ci --config=auto --sarif --output=semgrep.sarif --verbose

##@ Docker

docker-build: ## Build Docker image
	$(DOCKER) build -t $(IMAGE_NAME):$(IMAGE_TAG) .

docker-build-cache: ## Build Docker image with buildx cache
ifeq ($(DETECTED_OS),Windows)
	$(DOCKER) buildx build --cache-from type=local,src=%TEMP%\.buildx-cache --cache-to type=local,dest=%TEMP%\.buildx-cache -t $(IMAGE_NAME):$(IMAGE_TAG) .
else
	$(DOCKER) buildx build \
		--cache-from type=local,src=/tmp/.buildx-cache \
		--cache-to type=local,dest=/tmp/.buildx-cache \
		-t $(IMAGE_NAME):$(IMAGE_TAG) .
endif

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
ifeq ($(DETECTED_OS),Windows)
	@powershell -Command "Invoke-WebRequest -Uri http://localhost:$(PORT)/actuator/health -UseBasicParsing"
else
	@curl -f http://localhost:$(PORT)/actuator/health
endif

##@ CI/CD

ci-build: docker-build verify-image ## Run CI build pipeline locally

ci-test: test ## Run CI test pipeline locally

ci-format: format-check ## Run CI format check locally

ci-qodana: qodana ## Run CI Qodana pipeline locally

ci-semgrep: semgrep ## Run CI Semgrep pipeline locally

##@ Cleanup

clean: ## Clean build artifacts
	$(GRADLE) clean --no-daemon
ifeq ($(DETECTED_OS),Windows)
	@if exist build rmdir /s /q build
else
	@rm -rf build/
endif

clean-docker: docker-stop ## Remove Docker images and containers
	@$(DOCKER) rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true

clean-all: clean clean-docker ## Complete cleanup

##@ Setup

install: ## Install and verify development tools
ifeq ($(DETECTED_OS),Windows)
	@if not exist google-java-format-1.28.0-all-deps.jar ( \
		curl -L -o google-java-format-1.28.0-all-deps.jar https://github.com/google/google-java-format/releases/download/v1.28.0/google-java-format-1.28.0-all-deps.jar \
	)
	@if exist format.sh ( attrib +x format.sh 2>nul )
	@if exist yamlfmt ( attrib +x yamlfmt\* 2>nul )
else
	@test -f google-java-format-1.28.0-all-deps.jar || \
		curl -L -o google-java-format-1.28.0-all-deps.jar \
		https://github.com/google/google-java-format/releases/download/v1.28.0/google-java-format-1.28.0-all-deps.jar
	@if [ -f "format.sh" ]; then chmod +x format.sh; fi
	@if [ -d "yamlfmt" ]; then chmod +x yamlfmt/* 2>/dev/null || true; fi
endif

verify: ## Verify development environment
	@echo "Detected OS: $(DETECTED_OS)"
	@echo "Gradle command: $(GRADLE)"
	@java -version
	@$(GRADLE) --version
	@docker --version
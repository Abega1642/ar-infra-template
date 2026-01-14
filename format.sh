#!/bin/sh

# Formats Java, YAML, and Shell files using Google Java Format, yamlfmt, and shfmt
# Supports Linux, macOS, and Windows across multiple architectures

set -e

# Security: Ensure we're in the project directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

YAMLFMT_DIR="yamllint"
SHFMT_DIR="shfmt"
JAVA_FORMATTER="google-java-format-1.28.0-all-deps.jar"
JAVA_SOURCE_DIR="src"

if [ -t 1 ]; then
	RED='\033[0;31m'
	GREEN='\033[0;32m'
	YELLOW='\033[1;33m'
	BLUE='\033[0;34m'
	NC='\033[0m'
else
	RED=''
	GREEN=''
	YELLOW=''
	BLUE=''
	NC=''
fi

log_info() {
	printf "${BLUE}[INFO]${NC} %s\n" "$1"
}

log_success() {
	printf "${GREEN}[SUCCESS]${NC} %s\n" "$1"
}

log_warning() {
	printf "${YELLOW}[WARNING]${NC} %s\n" "$1"
}

log_error() {
	printf "${RED}[ERROR]${NC} %s\n" "$1"
}

detect_yamlfmt_binary() {
	OS=$(uname -s)
	ARCH=$(uname -m)

	case "$OS" in
	Linux)
		case "$ARCH" in
		x86_64 | amd64)
			echo "yamlfmt-linux-x86"
			;;
		aarch64 | arm64)
			echo "yamlfmt-linux-arm64"
			;;
		i386 | i686)
			log_error "32-bit Linux is not supported"
			echo "unsupported"
			;;
		*)
			log_error "Unsupported Linux architecture: $ARCH"
			echo "unsupported"
			;;
		esac
		;;
	Darwin)
		case "$ARCH" in
		x86_64 | amd64)
			echo "yamlfmt-darwin-x86"
			;;
		arm64)
			echo "yamlfmt-darwin-arm64"
			;;
		*)
			log_error "Unsupported macOS architecture: $ARCH"
			echo "unsupported"
			;;
		esac
		;;
	MINGW* | MSYS* | CYGWIN*)
		case "$ARCH" in
		x86_64 | amd64)
			echo "yamlfmt-x86.exe"
			;;
		aarch64 | arm64)
			echo "yamlfmt-arm64.exe"
			;;
		*)
			log_error "Unsupported Windows architecture: $ARCH"
			echo "unsupported"
			;;
		esac
		;;
	*)
		log_error "Unsupported operating system: $OS"
		echo "unsupported"
		;;
	esac
}

detect_shfmt_binary() {
	OS=$(uname -s)
	ARCH=$(uname -m)

	case "$OS" in
	Linux)
		case "$ARCH" in
		x86_64 | amd64)
			echo "shfmt_v3.12.0_linux_amd64"
			;;
		aarch64 | arm64)
			echo "shfmt_v3.12.0_linux_arm64"
			;;
		armv7l | armv6l)
			echo "shfmt_v3.12.0_linux_arm"
			;;
		i386 | i686)
			echo "shfmt_v3.12.0_linux_386"
			;;
		*)
			log_error "Unsupported Linux architecture: $ARCH"
			echo "unsupported"
			;;
		esac
		;;
	Darwin)
		case "$ARCH" in
		x86_64 | amd64)
			echo "shfmt_v3.12.0_darwin_amd64"
			;;
		arm64)
			echo "shfmt_v3.12.0_darwin_arm64"
			;;
		*)
			log_error "Unsupported macOS architecture: $ARCH"
			echo "unsupported"
			;;
		esac
		;;
	MINGW* | MSYS* | CYGWIN*)
		case "$ARCH" in
		x86_64 | amd64)
			echo "shfmt_v3.12.0_windows_amd64.exe"
			;;
		i386 | i686)
			echo "shfmt_v3.12.0_windows_386.exe"
			;;
		*)
			log_error "Unsupported Windows architecture: $ARCH"
			echo "unsupported"
			;;
		esac
		;;
	*)
		log_error "Unsupported operating system: $OS"
		echo "unsupported"
		;;
	esac
}

validate_environment() {
	if [ ! -d "$YAMLFMT_DIR" ]; then
		log_error "yamlfmt directory not found: $YAMLFMT_DIR"
		return 1
	fi

	if [ -L "$YAMLFMT_DIR" ]; then
		log_error "Security: $YAMLFMT_DIR is a symbolic link, which is not allowed"
		return 1
	fi

	if [ ! -d "$SHFMT_DIR" ]; then
		log_warning "shfmt directory not found: $SHFMT_DIR"
		log_warning "Skipping shell script formatting"
	fi

	if [ -L "$SHFMT_DIR" ]; then
		log_error "Security: $SHFMT_DIR is a symbolic link, which is not allowed"
		return 1
	fi

	if [ ! -f "$JAVA_FORMATTER" ]; then
		log_error "Java formatter not found: $JAVA_FORMATTER"
		return 1
	fi

	if [ ! -d "$JAVA_SOURCE_DIR" ]; then
		log_warning "Java source directory not found: $JAVA_SOURCE_DIR"
		log_warning "Skipping Java formatting"
		return 2
	fi

	return 0
}

format_java() {
	log_info "Formatting Java files..."

	java_files=$(find "$JAVA_SOURCE_DIR" -type f -name "*.java" 2>/dev/null | wc -l)

	if [ "$java_files" -eq 0 ]; then
		log_warning "No Java files found in $JAVA_SOURCE_DIR"
		return 0
	fi

	log_info "Found $java_files Java file(s)"

	if ! find "$JAVA_SOURCE_DIR" -type f -name "*.java" -print0 |
		xargs -0 -n 500 java -jar "$JAVA_FORMATTER" --replace 2>&1; then
		log_error "Java formatting failed"
		return 1
	fi

	log_success "Java files formatted successfully"
	return 0
}

format_yaml() {
	yamlfmt_binary="$1"
	yamlfmt_path="${YAMLFMT_DIR}/${yamlfmt_binary}"

	log_info "Formatting YAML files..."

	yamlfmt_realpath=$(cd "$(dirname "$yamlfmt_path")" && pwd)/$(basename "$yamlfmt_path")
	expected_dir=$(cd "$YAMLFMT_DIR" && pwd)

	case "$yamlfmt_realpath" in
	"$expected_dir/"*) ;;
	*)
		log_error "Security: yamlfmt binary path traversal detected"
		return 1
		;;
	esac

	if [ ! -f "$yamlfmt_path" ]; then
		log_error "yamlfmt binary not found: $yamlfmt_path"
		log_info "Available binaries in $YAMLFMT_DIR:"
		ls -1 "$YAMLFMT_DIR"/yamlfmt-* 2>/dev/null || log_info "  None found"
		return 1
	fi

	if [ -L "$yamlfmt_path" ]; then
		log_error "Security: yamlfmt binary is a symbolic link, which is not allowed"
		return 1
	fi

	chmod +x "$yamlfmt_path" 2>/dev/null || {
		log_warning "Could not set executable permissions on $yamlfmt_path"
	}

	yaml_files=$(find . -type f \( -name "*.yml" -o -name "*.yaml" \) ! -path "./.git/*" ! -path "./.*" 2>/dev/null | wc -l)

	if [ "$yaml_files" -eq 0 ]; then
		log_warning "No YAML files found"
		return 0
	fi

	log_info "Found $yaml_files YAML file(s)"

	if ! find . -type f \( -name "*.yml" -o -name "*.yaml" \) \
		! -path "./.git/*" ! -path "./.*" \
		-print0 | xargs -0 "$yamlfmt_path" 2>&1; then
		log_error "YAML formatting failed"
		return 1
	fi

	log_success "YAML files formatted successfully"
	return 0
}

format_shell() {
	shfmt_binary="$1"
	shfmt_path="${SHFMT_DIR}/${shfmt_binary}"

	log_info "Formatting shell script files..."

	shfmt_realpath=$(cd "$(dirname "$shfmt_path")" && pwd)/$(basename "$shfmt_path")
	expected_dir=$(cd "$SHFMT_DIR" && pwd)

	case "$shfmt_realpath" in
	"$expected_dir/"*) ;;
	*)
		log_error "Security: shfmt binary path traversal detected"
		return 1
		;;
	esac

	if [ ! -f "$shfmt_path" ]; then
		log_error "shfmt binary not found: $shfmt_path"
		log_info "Available binaries in $SHFMT_DIR:"
		ls -1 "$SHFMT_DIR"/shfmt_* 2>/dev/null || log_info "  None found"
		return 1
	fi

	if [ -L "$shfmt_path" ]; then
		log_error "Security: shfmt binary is a symbolic link, which is not allowed"
		return 1
	fi

	chmod +x "$shfmt_path" 2>/dev/null || {
		log_warning "Could not set executable permissions on $shfmt_path"
	}

	shell_files=$(find . -type f \( -name "*.sh" -o -name "*.bash" \) ! -path "./.git/*" ! -path "./.*" 2>/dev/null | wc -l)

	if [ "$shell_files" -eq 0 ]; then
		log_warning "No shell script files found"
		return 0
	fi

	log_info "Found $shell_files shell script file(s)"

	if ! "$shfmt_path" -l -w . 2>&1; then
		log_error "Shell script formatting failed"
		return 1
	fi

	log_success "Shell script files formatted successfully"
	return 0
}

main() {
	log_info "Starting code formatting for the current project"
	log_info "Platform: $(uname -s) $(uname -m)"

	validate_result=0
	validate_environment || validate_result=$?

	if [ $validate_result -eq 1 ]; then
		log_error "Environment validation failed"
		exit 1
	fi

	YAMLFMT_BINARY=$(detect_yamlfmt_binary)
	SHFMT_BINARY=$(detect_shfmt_binary)

	if [ "$YAMLFMT_BINARY" = "unsupported" ]; then
		log_warning "Platform not supported for yamlfmt"
		log_warning "OS: $(uname -s), Architecture: $(uname -m)"
		log_warning "Skipping YAML formatting"
	else
		log_info "Using yamlfmt binary: $YAMLFMT_BINARY"
	fi

	if [ "$SHFMT_BINARY" = "unsupported" ]; then
		log_warning "Platform not supported for shfmt"
		log_warning "OS: $(uname -s), Architecture: $(uname -m)"
		log_warning "Skipping shell script formatting"
	else
		log_info "Using shfmt binary: $SHFMT_BINARY"
	fi

	if [ $validate_result -ne 2 ]; then
		format_java || exit 1
	fi

	if [ "$YAMLFMT_BINARY" != "unsupported" ]; then
		format_yaml "$YAMLFMT_BINARY" || exit 1
	fi

	if [ "$SHFMT_BINARY" != "unsupported" ] && [ -d "$SHFMT_DIR" ]; then
		format_shell "$SHFMT_BINARY" || exit 1
	fi

	echo ""
	log_success "All formatting completed successfully"
}

main "$@"

#!/bin/sh

# Formats Java and YAML files using Google Java Format and yamlfmt
# Supports Linux, macOS, and Windows across multiple architectures

set -e

# Security: Ensure we're in the project directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

YAMLFMT_DIR="yamllint"
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
                x86_64|amd64)
                    echo "yamlfmt-linux-x86"
                    ;;
                aarch64|arm64)
                    echo "yamlfmt-linux-arm64"
                    ;;
                i386|i686)
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
                x86_64|amd64)
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
        MINGW*|MSYS*|CYGWIN*)
            case "$ARCH" in
                x86_64|amd64)
                    echo "yamlfmt-x86.exe"
                    ;;
                aarch64|arm64)
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

validate_environment() {
    if [ ! -d "$YAMLFMT_DIR" ]; then
        log_error "yamlfmt directory not found: $YAMLFMT_DIR"
        return 1
    fi
    
    if [ -L "$YAMLFMT_DIR" ]; then
        log_error "Security: $YAMLFMT_DIR is a symbolic link, which is not allowed"
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
    
    if ! find "$JAVA_SOURCE_DIR" -type f -name "*.java" -print0 | \
         xargs -0 -n 500 java -jar "$JAVA_FORMATTER" --replace 2>&1; then
        log_error "Java formatting failed"
        return 1
    fi
    
    log_success "Java files formatted successfully"
    return 0
}

format_yaml() {
    local yamlfmt_binary="$1"
    local yamlfmt_path="${YAMLFMT_DIR}/${yamlfmt_binary}"
    
    log_info "Formatting YAML files..."
    
    yamlfmt_realpath=$(cd "$(dirname "$yamlfmt_path")" && pwd)/$(basename "$yamlfmt_path")
    expected_dir=$(cd "$YAMLFMT_DIR" && pwd)
    
    case "$yamlfmt_realpath" in
        "$expected_dir/"*)
            ;;
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
    
    if [ "$YAMLFMT_BINARY" = "unsupported" ]; then
        log_warning "Platform not supported for yamlfmt"
        log_warning "OS: $(uname -s), Architecture: $(uname -m)"
        log_warning "Skipping YAML formatting, but continuing with Java formatting"
        
        if [ $validate_result -ne 2 ]; then
            format_java || exit 1
        fi
        exit 0
    fi
    
    log_info "Using yamlfmt binary: $YAMLFMT_BINARY"
    
    if [ $validate_result -ne 2 ]; then
        format_java || exit 1
    fi
    
    format_yaml "$YAMLFMT_BINARY" || exit 1
    
    echo ""
    log_success "All formatting completed successfully"
}

main "$@"

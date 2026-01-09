@echo off
setlocal enabledelayedexpansion

REM Formats Java and YAML files using Google Java Format and yamlfmt
REM Windows-native batch script

REM Security: Ensure we're in the project directory
cd /d "%~dp0" || exit /b 1

set "YAMLFMT_DIR=yamllint"
set "JAVA_FORMATTER=google-java-format-1.28.0-all-deps.jar"
set "JAVA_SOURCE_DIR=src"
set "EXIT_CODE=0"

REM Color codes for Windows (requires ANSI support in Windows 10+)
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM Check if running in a terminal that supports ANSI
echo.|set /p=""> nul 2>&1
if errorlevel 1 (
    set "RED="
    set "GREEN="
    set "YELLOW="
    set "BLUE="
    set "NC="
)

goto :main

:log_info
    echo %BLUE%[INFO]%NC% %~1
    exit /b 0

:log_success
    echo %GREEN%[SUCCESS]%NC% %~1
    exit /b 0

:log_warning
    echo %YELLOW%[WARNING]%NC% %~1
    exit /b 0

:log_error
    echo %RED%[ERROR]%NC% %~1
    exit /b 0

:detect_yamlfmt_binary
    set "ARCH=%PROCESSOR_ARCHITECTURE%"

    if /i "%ARCH%"=="AMD64" (
        set "YAMLFMT_BINARY=yamlfmt-x86.exe"
    ) else if /i "%ARCH%"=="ARM64" (
        set "YAMLFMT_BINARY=yamlfmt-arm64.exe"
    ) else (
        call :log_error "Unsupported Windows architecture: %ARCH%"
        set "YAMLFMT_BINARY=unsupported"
    )
    exit /b 0

:validate_environment
    if not exist "%YAMLFMT_DIR%" (
        call :log_error "yamlfmt directory not found: %YAMLFMT_DIR%"
        exit /b 1
    )

    if not exist "%JAVA_FORMATTER%" (
        call :log_error "Java formatter not found: %JAVA_FORMATTER%"
        exit /b 1
    )

    if not exist "%JAVA_SOURCE_DIR%" (
        call :log_warning "Java source directory not found: %JAVA_SOURCE_DIR%"
        call :log_warning "Skipping Java formatting"
        exit /b 2
    )

    exit /b 0

:format_java
    call :log_info "Formatting Java files..."

    set "JAVA_FILE_COUNT=0"
    for /r "%JAVA_SOURCE_DIR%" %%f in (*.java) do (
        set /a JAVA_FILE_COUNT+=1
    )

    if %JAVA_FILE_COUNT% equ 0 (
        call :log_warning "No Java files found in %JAVA_SOURCE_DIR%"
        exit /b 0
    )

    call :log_info "Found %JAVA_FILE_COUNT% Java file(s)"

    REM Create temporary file list
    set "TEMP_LIST=%TEMP%\java_files_%RANDOM%.txt"
    dir /s /b "%JAVA_SOURCE_DIR%\*.java" > "%TEMP_LIST%" 2>nul

    REM Process files in batches of 500
    set "BATCH_SIZE=500"
    set "COUNT=0"
    set "BATCH_FILES="

    for /f "delims=" %%f in (%TEMP_LIST%) do (
        set "BATCH_FILES=!BATCH_FILES! "%%f""
        set /a COUNT+=1

        if !COUNT! geq %BATCH_SIZE% (
            java -jar "%JAVA_FORMATTER%" --replace !BATCH_FILES! 2>&1
            if errorlevel 1 (
                del "%TEMP_LIST%" 2>nul
                call :log_error "Java formatting failed"
                exit /b 1
            )
            set "BATCH_FILES="
            set "COUNT=0"
        )
    )

    REM Process remaining files
    if defined BATCH_FILES (
        java -jar "%JAVA_FORMATTER%" --replace !BATCH_FILES! 2>&1
        if errorlevel 1 (
            del "%TEMP_LIST%" 2>nul
            call :log_error "Java formatting failed"
            exit /b 1
        )
    )

    del "%TEMP_LIST%" 2>nul
    call :log_success "Java files formatted successfully"
    exit /b 0

:format_yaml
    set "YAMLFMT_PATH=%YAMLFMT_DIR%\%YAMLFMT_BINARY%"

    call :log_info "Formatting YAML files..."

    if not exist "%YAMLFMT_PATH%" (
        call :log_error "yamlfmt binary not found: %YAMLFMT_PATH%"
        call :log_info "Available binaries in %YAMLFMT_DIR%:"
        dir /b "%YAMLFMT_DIR%\yamlfmt-*" 2>nul || call :log_info "  None found"
        exit /b 1
    )

    REM Count YAML files
    set "YAML_FILE_COUNT=0"
    for /r %%f in (*.yml *.yaml) do (
        set "FILEPATH=%%f"
        echo !FILEPATH! | findstr /i /c:".git" >nul
        if errorlevel 1 (
            set /a YAML_FILE_COUNT+=1
        )
    )

    if %YAML_FILE_COUNT% equ 0 (
        call :log_warning "No YAML files found"
        exit /b 0
    )

    call :log_info "Found %YAML_FILE_COUNT% YAML file(s)"

    REM Create temporary file list
    set "TEMP_YAML_LIST=%TEMP%\yaml_files_%RANDOM%.txt"
    (
        for /r %%f in (*.yml *.yaml) do (
            set "FILEPATH=%%f"
            echo !FILEPATH! | findstr /i /c:".git" >nul
            if errorlevel 1 echo %%f
        )
    ) > "%TEMP_YAML_LIST%"

    REM Format YAML files
    for /f "delims=" %%f in (%TEMP_YAML_LIST%) do (
        "%YAMLFMT_PATH%" "%%f" 2>&1
        if errorlevel 1 (
            del "%TEMP_YAML_LIST%" 2>nul
            call :log_error "YAML formatting failed"
            exit /b 1
        )
    )

    del "%TEMP_YAML_LIST%" 2>nul
    call :log_success "YAML files formatted successfully"
    exit /b 0

:main
    call :log_info "Starting code formatting for the current project"

    for /f "tokens=*" %%i in ('ver') do set "WIN_VER=%%i"
    call :log_info "Platform: Windows %PROCESSOR_ARCHITECTURE%"

    call :validate_environment
    set "VALIDATE_RESULT=%errorlevel%"

    if %VALIDATE_RESULT% equ 1 (
        call :log_error "Environment validation failed"
        exit /b 1
    )

    call :detect_yamlfmt_binary

    if "%YAMLFMT_BINARY%"=="unsupported" (
        call :log_warning "Platform not supported for yamlfmt"
        call :log_warning "Architecture: %PROCESSOR_ARCHITECTURE%"
        call :log_warning "Skipping YAML formatting, but continuing with Java formatting"

        if %VALIDATE_RESULT% neq 2 (
            call :format_java
            if errorlevel 1 exit /b 1
        )
        exit /b 0
    )

    call :log_info "Using yamlfmt binary: %YAMLFMT_BINARY%"

    if %VALIDATE_RESULT% neq 2 (
        call :format_java
        if errorlevel 1 exit /b 1
    )

    call :format_yaml
    if errorlevel 1 exit /b 1

    echo.
    call :log_success "All formatting completed successfully"
    exit /b 0
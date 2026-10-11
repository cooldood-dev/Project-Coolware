@echo off
setlocal enabledelayedexpansion
title Coolware Client Launcher (MCP 1.8.9)

echo ===================================================
echo           Coolware v2 - Client Launcher
echo ===================================================
echo.

:: ----------------------------------------------------
:: 1. Resolve Project Root and MavenMCP Directory
:: ----------------------------------------------------
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

if exist "%SCRIPT_DIR%\pom.xml" (
    set "MCP_DIR=%SCRIPT_DIR%"
    set "ROOT_DIR=%SCRIPT_DIR%\.."
) else if exist "%SCRIPT_DIR%\MavenMCP-1.8.9-master\pom.xml" (
    set "MCP_DIR=%SCRIPT_DIR%\MavenMCP-1.8.9-master"
    set "ROOT_DIR=%SCRIPT_DIR%"
) else (
    set "MCP_DIR=%SCRIPT_DIR%\MavenMCP-1.8.9-master"
    set "ROOT_DIR=%SCRIPT_DIR%"
)

echo [*] MCP Directory: %MCP_DIR%

:: ----------------------------------------------------
:: 2. Auto-Detect Java 8 runtime
:: ----------------------------------------------------
echo [*] Detecting Java 8 runtime...
set "JAVA8_BIN="
set "JAVA8_HOME="

:: Check JAVA_HOME if set to 1.8
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /i /c:"1.8" /c:"\"8" >nul
        if !errorlevel! equ 0 (
            set "JAVA8_BIN=%JAVA_HOME%\bin\java.exe"
            set "JAVA8_HOME=%JAVA_HOME%"
        )
    )
)

:: Search common JDK 8 / JRE 8 installation locations
if not defined JAVA8_BIN (
    for /d %%D in (
        "C:\Program Files\Eclipse Adoptium\jdk-8*"
        "C:\Program Files\Eclipse Adoptium\jre-8*"
        "C:\Program Files\Java\jdk1.8*"
        "C:\Program Files\Java\jre1.8*"
        "C:\Program Files (x86)\Java\jdk1.8*"
        "C:\Program Files (x86)\Java\jre1.8*"
        "C:\Program Files\BellSoft\LibericaJDK-8*"
        "C:\Program Files\Zulu\zulu-8*"
        "C:\Program Files\Amazon Corretto\jdk1.8*"
        "%USERPROFILE%\.jdks\*1.8*"
        "%USERPROFILE%\.jdks\corretto-1.8*"
        "%USERPROFILE%\.jdks\temurin-1.8*"
        "%USERPROFILE%\.jdks\zulu-8*"
    ) do (
        if not defined JAVA8_BIN (
            if exist "%%~D\bin\java.exe" (
                set "JAVA8_BIN=%%~D\bin\java.exe"
                set "JAVA8_HOME=%%~D"
            )
        )
    )
)

:: Fallback check default system 'java' command
if not defined JAVA8_BIN (
    where java.exe >nul 2>&1
    if !errorlevel! equ 0 (
        for /f "tokens=*" %%V in ('java -version 2^>^&1') do (
            echo "%%V" | findstr /i /c:"1.8" /c:"\"8" >nul
            if !errorlevel! equ 0 (
                for /f "delims=" %%P in ('where java.exe') do (
                    if not defined JAVA8_BIN set "JAVA8_BIN=%%P"
                )
            )
        )
    )
)

if not defined JAVA8_BIN (
    echo [ERROR] Java 8 runtime could not be found!
    echo         Minecraft 1.8.9 requires Java 8 to run.
    echo         Please install Adoptium Temurin JRE or JDK 8 from:
    echo         https://adoptium.net/temurin/releases/?version=8
    echo.
    pause
    exit /b 1
)

echo [OK] Using Java 8 runtime: %JAVA8_BIN%

:: ----------------------------------------------------
:: 3. Auto-Detect Maven
:: ----------------------------------------------------
set "MVN_CMD="
if exist "C:\apache-maven-3.9.16\bin\mvn.cmd" (
    set "MVN_CMD=C:\apache-maven-3.9.16\bin\mvn.cmd"
) else if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
) else if defined M2_HOME (
    if exist "%M2_HOME%\bin\mvn.cmd" set "MVN_CMD=%M2_HOME%\bin\mvn.cmd"
) else (
    where mvn.cmd >nul 2>&1
    if !errorlevel! equ 0 (
        set "MVN_CMD=mvn.cmd"
    )
)

:: ----------------------------------------------------
:: 4. Auto-Detect Minecraft Directory & Assets
:: ----------------------------------------------------
echo [*] Detecting Minecraft Assets ^& Game Directory...

set "MINECRAFT_DIR="
if exist "%APPDATA%\.minecraft" (
    set "MINECRAFT_DIR=%APPDATA%\.minecraft"
) else if exist "%MCP_DIR%\test_run" (
    set "MINECRAFT_DIR=%MCP_DIR%\test_run"
) else (
    set "MINECRAFT_DIR=%MCP_DIR%\test_run"
    mkdir "%MINECRAFT_DIR%" 2>nul
)

set "ASSETS_DIR="
if exist "%MCP_DIR%\test_run\assets\indexes\1.8.json" (
    set "ASSETS_DIR=%MCP_DIR%\test_run\assets"
) else if exist "%APPDATA%\.minecraft\assets\indexes\1.8.json" (
    set "ASSETS_DIR=%APPDATA%\.minecraft\assets"
) else if exist "%MCP_DIR%\test_run\assets" (
    set "ASSETS_DIR=%MCP_DIR%\test_run\assets"
) else if exist "%APPDATA%\.minecraft\assets" (
    set "ASSETS_DIR=%APPDATA%\.minecraft\assets"
) else (
    set "ASSETS_DIR=%MCP_DIR%\test_run\assets"
)

set "NATIVES_DIR=%MCP_DIR%\test_natives\windows"

echo [OK] Game Directory:   %MINECRAFT_DIR%
echo [OK] Assets Directory: %ASSETS_DIR%
echo [OK] Natives:          %NATIVES_DIR%

:: ----------------------------------------------------
:: 5. Locate or Build Pre-Built Binaries
:: ----------------------------------------------------
set "CLIENT_JAR=%MCP_DIR%\target\MCP-1.8.9-jar-with-dependencies.jar"
if not exist "%CLIENT_JAR%" (
    if exist "%MCP_DIR%\target\Coolware.jar" (
        set "CLIENT_JAR=%MCP_DIR%\target\Coolware.jar"
    )
)

set "BUILD_NEEDED="
if not exist "%CLIENT_JAR%" (
    set "BUILD_NEEDED=1"
) else (
    powershell -NoProfile -Command "$jar = Get-Item -LiteralPath '%CLIENT_JAR%'; $source = Get-ChildItem -LiteralPath '%MCP_DIR%\src\main' -Recurse -File | Where-Object { $_.LastWriteTime -gt $jar.LastWriteTime } | Select-Object -First 1; $pom = Get-Item -LiteralPath '%MCP_DIR%\pom.xml'; if ($source -or $pom.LastWriteTime -gt $jar.LastWriteTime) { exit 1 } else { exit 0 }"
    if !errorlevel! neq 0 set "BUILD_NEEDED=1"
)

if defined BUILD_NEEDED (
    echo.
    echo [*] Client JAR is missing or source files have changed:
    echo     %CLIENT_JAR%
    echo [*] Building project using Maven...
    
    if not defined MVN_CMD (
        echo [ERROR] Maven not found. Cannot build client JAR.
        echo         Please place Maven at C:\apache-maven-3.9.16 or add it to PATH.
        pause
        exit /b 1
    )

    :: Maven compiler detection (check Java 8 JDK or any installed JDK)
    set "BUILD_JAVA_HOME="
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\javac.exe" set "BUILD_JAVA_HOME=%JAVA_HOME%"
    )
    if not defined BUILD_JAVA_HOME (
        if defined JAVA8_HOME (
            if exist "%JAVA8_HOME%\bin\javac.exe" set "BUILD_JAVA_HOME=%JAVA8_HOME%"
        )
    )
    if not defined BUILD_JAVA_HOME (
        for /d %%D in (
            "C:\Program Files\Eclipse Adoptium\jdk-8*"
            "C:\Program Files\Java\jdk1.8*"
            "C:\Program Files\BellSoft\LibericaJDK-8*"
            "C:\Program Files\Zulu\zulu-8*"
            "C:\Program Files\Amazon Corretto\jdk1.8*"
            "C:\Program Files\Java\jdk-21*"
            "C:\Program Files\Java\jdk-25*"
            "C:\Program Files\Eclipse Adoptium\jdk-21*"
            "C:\Program Files\Eclipse Adoptium\jdk-25*"
            "C:\Program Files\Microsoft\jdk-21*"
            "C:\Program Files\Microsoft\jdk-25*"
            "C:\Program Files\Java\jdk-*"
            "C:\Program Files\Eclipse Adoptium\jdk-*"
            "C:\Program Files\BellSoft\LibericaJDK-*"
            "C:\Program Files\Zulu\zulu-*"
            "C:\Program Files\Amazon Corretto\jdk-*"
            "%USERPROFILE%\.jdks\*"
        ) do (
            if not defined BUILD_JAVA_HOME (
                if exist "%%~D\bin\javac.exe" set "BUILD_JAVA_HOME=%%~D"
            )
        )
    )

    if not defined BUILD_JAVA_HOME (
        echo [ERROR] A JDK with javac compiler is required to build the client.
        echo         Please install Adoptium Temurin JDK 8 or newer from:
        echo         https://adoptium.net/temurin/releases/?version=8
        pause
        exit /b 1
    )

    echo [*] Building with JDK: !BUILD_JAVA_HOME!
    set "JAVA_HOME=!BUILD_JAVA_HOME!"
    set "PATH=!BUILD_JAVA_HOME!\bin;%PATH%"
    
    call "%MVN_CMD%" -f "%MCP_DIR%\pom.xml" clean package -DskipTests
    if !errorlevel! neq 0 (
        echo [ERROR] Maven build failed!
        pause
        exit /b 1
    )

    :: Sync binaries and JSON
    if exist "%MCP_DIR%\target\MCP-1.8.9-jar-with-dependencies.jar" (
        copy /y "%MCP_DIR%\target\MCP-1.8.9-jar-with-dependencies.jar" "%MCP_DIR%\target\Coolware.jar" >nul 2>&1
        if exist "%ROOT_DIR%\versions\Coolware" (
            copy /y "%MCP_DIR%\target\MCP-1.8.9-jar-with-dependencies.jar" "%ROOT_DIR%\versions\Coolware\Coolware.jar" >nul 2>&1
        )
        if exist "%MCP_DIR%\Coolware.json" (
            copy /y "%MCP_DIR%\Coolware.json" "%MCP_DIR%\target\Coolware.json" >nul 2>&1
            if exist "%ROOT_DIR%\versions\Coolware" (
                copy /y "%MCP_DIR%\Coolware.json" "%ROOT_DIR%\versions\Coolware\Coolware.json" >nul 2>&1
            )
        )
    )
)

if not exist "%CLIENT_JAR%" (
    echo [ERROR] Could not find client jar: %CLIENT_JAR%
    pause
    exit /b 1
)

echo [OK] Client Binary:    %CLIENT_JAR%
if exist "%MCP_DIR%\target\Coolware.json" (
    echo [OK] Version JSON:     %MCP_DIR%\target\Coolware.json
)
echo.
echo ===================================================
echo              Launching Coolware Client...
echo ===================================================
echo.

:: ----------------------------------------------------
:: 6. Launch Minecraft Client Directly (Unobfuscated & No Auth)
:: ----------------------------------------------------
set "USERNAME_ARG=Player"
if defined USERNAME set "USERNAME_ARG=%USERNAME%"

cd /d "%MINECRAFT_DIR%"

"%JAVA8_BIN%" ^
  -Xms1024M ^
  -Xmx2048M ^
  "-Djava.library.path=%NATIVES_DIR%" ^
  "-Dorg.lwjgl.librarypath=%NATIVES_DIR%" ^
  "-Dnet.java.games.input.librarypath=%NATIVES_DIR%" ^
  -Dcoolware.noauth=true ^
  -cp "%CLIENT_JAR%" ^
  net.minecraft.client.main.Main ^
  --version "Coolware" ^
  --accessToken "0" ^
  --username "%USERNAME_ARG%" ^
  --gameDir "%MINECRAFT_DIR%" ^
  --assetsDir "%ASSETS_DIR%" ^
  --assetIndex "1.8" ^
  --userProperties "{}"

if %errorlevel% neq 0 (
    echo.
    echo [!] Client exited with code %errorlevel%
    pause
)

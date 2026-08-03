@echo off
REM ============================================================
REM  WildDifficulty — Multi-Version Release Build Script
REM  Compiles versioned JARs for Paper / Spigot 1.19 up to 26.2
REM ============================================================

SET JAVA_HOME=C:\Program Files\Amazon Corretto\jdk25.0.3_9
SET MVN=%TEMP%\maven-extract\apache-maven-3.9.9\bin\mvn.cmd

IF NOT EXIST "%MVN%" (
    echo [ERROR] Maven not found in %TEMP%\maven-extract\
    pause
    exit /b 1
)

IF NOT EXIST "releases" mkdir releases

echo [1/6] Building WildDifficulty paper-26.2...
"%MVN%" clean package -P v26_2 -DskipTests -q

echo [2/6] Building WildDifficulty paper-26.1...
"%MVN%" package -P v26_1 -DskipTests -q

echo [3/6] Building WildDifficulty paper-1.21...
"%MVN%" package -P v1_21 -DskipTests -q

echo [4/6] Building WildDifficulty paper-1.20.6...
"%MVN%" package -P v1_20_6 -DskipTests -q

echo [5/6] Building WildDifficulty paper-1.20...
"%MVN%" package -P v1_20 -DskipTests -q

echo [6/6] Building WildDifficulty paper-1.19...
"%MVN%" package -P v1_19 -DskipTests -q

echo.
echo [COPYING ALL JARS TO releases/ DIRECTORY...]
copy /Y "target\WildDifficulty-paper-*.jar" "releases\"

echo.
echo ============================================================
echo  [SUCCESS] All version JARs built and available in releases/
echo ============================================================
dir releases

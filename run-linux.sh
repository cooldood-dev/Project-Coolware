#!/usr/bin/env bash
# Coolware on Linux, without Maven: compiles src/main/java (minus what the pom
# excludes) into target/linux-classes, then starts the client with those
# classes ahead of the pre-built jar, which supplies the libraries and
# resources. Needs a Java 8 JDK on PATH.
set -euo pipefail

MCP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$MCP_DIR/target/MCP-1.8.9-jar-with-dependencies.jar"
CLASSES="$MCP_DIR/target/linux-classes"
NATIVES="$MCP_DIR/test_natives/linux"
GAME_DIR="$MCP_DIR/test_run"

# Assets: the project's own, else Prism's, else the vanilla launcher's.
ASSETS="$MCP_DIR/test_run/assets"
for candidate in "$ASSETS" "$HOME/.local/share/PrismLauncher/assets" "$HOME/.minecraft/assets"; do
    if [ -f "$candidate/indexes/1.8.json" ]; then
        ASSETS="$candidate"
        break
    fi
done

if [ ! -f "$JAR" ]; then
    echo "Missing $JAR (it supplies the libraries)." >&2
    exit 1
fi

echo "[*] Compiling..."
rm -rf "$CLASSES"
mkdir -p "$CLASSES" "$GAME_DIR"
# Relative paths: javac's @file splits on spaces, and this folder's path may have some.
cd "$MCP_DIR"
find src/main/java -name "*.java" -not -path "*/net/montoyo/*" -not -path "*/org/cef/*" > target/linux-sources.txt
javac -nowarn -encoding UTF-8 -source 1.8 -target 1.8 -cp "$JAR" -d "$CLASSES" @target/linux-sources.txt

echo "[*] Launching (assets: $ASSETS)..."
cd "$GAME_DIR"
exec java -Xms1024M -Xmx2048M \
    "-Djava.library.path=$NATIVES" \
    "-Dorg.lwjgl.librarypath=$NATIVES" \
    "-Dnet.java.games.input.librarypath=$NATIVES" \
    -cp "$CLASSES:$JAR" \
    net.minecraft.client.main.Main \
    --version Coolware \
    --accessToken 0 \
    --username "${COOLWARE_USER:-Player}" \
    --gameDir "$GAME_DIR" \
    --assetsDir "$ASSETS" \
    --assetIndex 1.8 \
    --userProperties "{}"

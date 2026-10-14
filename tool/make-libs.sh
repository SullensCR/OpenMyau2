#!/usr/bin/env bash
# Regenerate lib/ (gitignored) from local game/caches jars.
# Nothing in lib/ is committed; run this after cloning, then ./gradlew build.
#
# Sources (override with env):
#   FORGE_SRG_DIR  ForgeGradle cache dir holding srgBin + forgeBin jars
#                  (default: ~/.gradle/caches/.../stable/22, fallback stable/20)
#   PRISM_LIBS     PrismLauncher libraries dir (lwjgl/authlib/guava/log4j/netty)
#   OPENJADE_LIB   dir with annotations-java5 + recovered-libraries jars (optional)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIB="$ROOT/lib"
TOOL="$ROOT/tool"
BUILD="$TOOL/.build"
mkdir -p "$LIB" "$BUILD"

MC="$HOME/.gradle/caches/minecraft/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9"
SRG_DIR="${FORGE_SRG_DIR:-$MC/stable/22}"
[ -d "$SRG_DIR" ] || SRG_DIR="$MC/stable/20"
SRG="$SRG_DIR/forge-1.8.9-11.15.1.2318-1.8.9-srgBin.jar"
FORGEBIN="$SRG_DIR/forgeBin-1.8.9-11.15.1.2318-1.8.9.jar"
[ -f "$SRG" ] || { echo "missing: $SRG (set FORGE_SRG_DIR)" >&2; exit 1; }
[ -f "$FORGEBIN" ] || { echo "missing: $FORGEBIN (set FORGE_SRG_DIR)" >&2; exit 1; }
cp -p "$SRG" "$LIB/forge-1.8.9-11.15.1.2318-1.8.9-srgBin.jar"
cp -p "$FORGEBIN" "$LIB/forgeBin-1.8.9.jar"

PRISM="${PRISM_LIBS:-/Volumes/SSD/PrismLauncher/libraries}"
need() { [ -f "$1" ] || { echo "missing: $1 (set PRISM_LIBS)" >&2; exit 1; }; cp -p "$1" "$LIB/$2"; }
need "$PRISM/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar" lwjgl-2.9.4-nightly-20150209.jar
need "$PRISM/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar" lwjgl_util-2.9.4-nightly-20150209.jar
need "$PRISM/com/mojang/authlib/1.5.21/authlib-1.5.21.jar" authlib-1.5.21.jar
need "$PRISM/com/google/guava/guava/17.0/guava-17.0.jar" guava-17.0.jar
need "$PRISM/org/apache/logging/log4j/log4j-api/2.0-beta9-fixed/log4j-api-2.0-beta9-fixed.jar" log4j-api-2.0-beta9.jar
need "$PRISM/io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar" netty-all-4.0.23.Final.jar

OJ="${OPENJADE_LIB:-/Volumes/SSD/scratch/OpenJade/lib}"
for j in annotations-java5-24.1.0.jar recovered-libraries.jar; do
  if [ -f "$OJ/$j" ]; then cp -p "$OJ/$j" "$LIB/$j"; else echo "warn: $OJ/$j not found, skipping" >&2; fi
done

ASM_JAR="$(find "$HOME/.gradle/wrapper" -name 'asm-9*.jar' -not -name '*tree*' -not -name '*analysis*' -not -name '*commons*' 2>/dev/null | head -n 1)"
ASM_TREE="$(find "$HOME/.gradle/wrapper" -name 'asm-tree-9*.jar' 2>/dev/null | head -n 1)"
[ -n "$ASM_JAR" ] && [ -n "$ASM_TREE" ] || { echo "ASM jars not found under ~/.gradle/wrapper" >&2; exit 1; }
javac -proc:none -cp "$ASM_JAR:$ASM_TREE" -d "$BUILD" "$TOOL/MergeSrgs.java"
java -cp "$BUILD:$ASM_JAR:$ASM_TREE" MergeSrgs \
  "$LIB/forge-1.8.9-11.15.1.2318-1.8.9-srgBin.jar" \
  "$LIB/forgeBin-1.8.9.jar" \
  "$LIB/merged-srg-forge.jar"

echo "--- lib/ ---"
ls -lh "$LIB"
javap -classpath "$LIB/merged-srg-forge.jar" net.minecraft.item.Item 2>&1 | grep -c 'func_\|getToolClasses'

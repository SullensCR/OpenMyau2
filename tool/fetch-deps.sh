#!/usr/bin/env bash
# Fetch + build compile-only deps from PUBLIC artifacts (used by CI).
# Local dev can use tool/make-libs.sh instead (ForgeGradle cache, faster).
# Output: lib/ (gitignored). Needs: curl, python3, javac, java.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIB="$ROOT/lib"
TOOL="$ROOT/tool"
DEPS="$TOOL/.deps"
BUILD="$TOOL/.build"
MCV="${MCV:-1.8.9}"
FORGEV="${FORGEV:-1.8.9-11.15.1.2318-1.8.9}"
ASMVER="${ASMVER:-9.9}"
mkdir -p "$LIB" "$DEPS" "$BUILD"

FORGE_MAVEN="https://maven.minecraftforge.net"
LIBRARIES_MC="https://libraries.minecraft.net"
CENTRAL="https://repo1.maven.org/maven2"
fetch() { # url dest
  [ -f "$2" ] || curl -fsSL --retry 3 -o "$2" "$1"
}

echo "== mojang 1.8.9 jars =="
python3 - "$DEPS" "$MCV" <<'EOF'
import json, sys, urllib.request
deps, mcv = sys.argv[1], sys.argv[2]
manifest = json.load(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
ver = next(v for v in manifest["versions"] if v["id"] == mcv)
pkg = json.load(urllib.request.urlopen(ver["url"]))
urls = [(s, pkg["downloads"][s]["url"]) for s in ("client", "server")]
json.dump(urls, open(f"{deps}/mc_urls.json", "w"))
print("ok")
EOF
python3 -c "
import json
for side, url in json.load(open('$DEPS/mc_urls.json')):
    print(side, url)
" | while read -r side url; do fetch "$url" "$DEPS/minecraft-$MCV-$side.jar"; done

echo "== mappings + forge + small jars =="
fetch "$FORGE_MAVEN/de/oceanlabs/mcp/mcp/1.8.9/mcp-1.8.9-srg.zip" "$DEPS/mcp-1.8.9-srg.zip"
fetch "$FORGE_MAVEN/net/minecraftforge/forge/$FORGEV/forge-$FORGEV-universal.jar" "$DEPS/forge-$FORGEV-universal.jar"
fetch "$LIBRARIES_MC/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar" "$LIB/lwjgl-2.9.4-nightly-20150209.jar"
fetch "$LIBRARIES_MC/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar" "$LIB/lwjgl_util-2.9.4-nightly-20150209.jar"
fetch "$LIBRARIES_MC/com/mojang/authlib/1.5.21/authlib-1.5.21.jar" "$LIB/authlib-1.5.21.jar"
for a in asm asm-tree asm-commons; do
  fetch "$CENTRAL/org/ow2/asm/$a/$ASMVER/$a-$ASMVER.jar" "$DEPS/$a-$ASMVER.jar"
done
unzip -o -q "$DEPS/mcp-1.8.9-srg.zip" joined.srg -d "$DEPS"
unzip -p "$DEPS/forge-$FORGEV-universal.jar" forge_at.cfg > "$DEPS/forge_at.cfg"

echo "== build tools =="
ASM_CP="$DEPS/asm-$ASMVER.jar:$DEPS/asm-tree-$ASMVER.jar:$DEPS/asm-commons-$ASMVER.jar"
javac -proc:none -cp "$ASM_CP" -d "$BUILD" "$TOOL/SrgDeobf.java" "$TOOL/MergeSrgs.java" "$TOOL/ApplyAt.java" "$TOOL/AddStubs.java"

echo "== notch merge + deobf =="
python3 - "$DEPS" "$MCV" <<'EOF'
import sys, zipfile
deps, mcv = sys.argv[1], sys.argv[2]
entries = {}
for side in ("server", "client"):
    z = zipfile.ZipFile(f"{deps}/minecraft-{mcv}-{side}.jar")
    for n in z.namelist():
        if n.endswith("/") or "META-INF/" in n:
            continue
        entries[n] = z.read(n)
w = zipfile.ZipFile(f"{deps}/notch-merged.jar", "w", zipfile.ZIP_DEFLATED)
for n, d in entries.items():
    w.writestr(n, d)
w.close()
print("merged entries:", len(entries))
EOF
java -cp "$BUILD:$ASM_CP" SrgDeobf "$DEPS/notch-merged.jar" "$DEPS/joined.srg" "$DEPS/srg-mc.jar"
java -cp "$BUILD:$ASM_CP" SrgDeobf "$DEPS/forge-$FORGEV-universal.jar" "$DEPS/joined.srg" "$DEPS/forge-srg.jar"
java -cp "$BUILD:$ASM_CP" MergeSrgs "$DEPS/srg-mc.jar" "$DEPS/forge-srg.jar" "$DEPS/merged1.jar"
java -cp "$BUILD:$ASM_CP" ApplyAt "$DEPS/merged1.jar" "$DEPS/forge_at.cfg" "$DEPS/merged2.jar"
java -cp "$BUILD:$ASM_CP" AddStubs "$DEPS/merged2.jar" "$TOOL/forge-vanilla-members.txt" "$LIB/merged-srg-forge.jar"

echo "--- lib/ ---"
ls -lh "$LIB"
javap -classpath "$LIB/merged-srg-forge.jar" net.minecraft.item.Item 2>&1 | grep -c "func_\|getToolClasses"

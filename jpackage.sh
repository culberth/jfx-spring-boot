#!/usr/bin/env bash
# Builds a standalone Linux executable (app-image) for jfx-playground using jpackage.
# Requires JDK 14+ (jpackage is bundled with the JDK). Run from the repo root:
#   ./jpackage.sh
#
# Output:
#   target/dist/JfxSpringBoot/bin/JfxSpringBoot   (self-contained, bundles its own JRE)

set -euo pipefail

APP_NAME="JfxSpringBoot"
APP_VERSION="0.0.1"
MAIN_CLASS="com.example.jfx.spring.MainApplication"
JAR_NAME="jfx-playground-0.0.1-SNAPSHOT.jar"

echo "Building application jar..."
./mvnw -q clean package -DskipTests

echo "Assembling jpackage input directory..."
INPUT_DIR="target/jpackage-input"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"

./mvnw -q dependency:copy-dependencies "-DoutputDirectory=$INPUT_DIR" -DincludeScope=runtime

# Lombok is compile-time only (annotation processing); it's harmless but unused at
# runtime, so drop it from the bundled app rather than fight -DexcludeGroupIds.
rm -f "$INPUT_DIR"/lombok-*.jar

# The Spring Boot plugin renames the plain (thin) jar to *.jar.original and replaces
# the original name with the fat/executable jar. jpackage needs the thin jar plus the
# dependency jars copied above, flattened into one directory, on its plain classpath.
cp "target/$JAR_NAME.original" "$INPUT_DIR/$JAR_NAME"

echo "Running jpackage..."
DEST_DIR="target/dist"
rm -rf "$DEST_DIR"

jpackage \
    --type app-image \
    --input "$INPUT_DIR" \
    --dest "$DEST_DIR" \
    --name "$APP_NAME" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --app-version "$APP_VERSION" \
    --vendor "eljaiek" \
    --description "Demo project for Spring Boot and JavaFX"

echo "Done:"
echo "  $DEST_DIR/$APP_NAME/bin/$APP_NAME"

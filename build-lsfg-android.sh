#!/usr/bin/env bash
# Build script for lsfg-vk Vulkan layer for Android ARM64
#
# Prerequisites:
#   - Android NDK (set ANDROID_NDK_HOME or pass --ndk)
#   - CMake 3.22+
#   - Ninja (optional, uses Make by default)
#
# Usage:
#   ./build-lsfg-android.sh [--ndk /path/to/ndk] [--clean] [--skip-if-exists]

set -euo pipefail

NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK:-}}"
CLEAN=false
SKIP_IF_EXISTS=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ndk) NDK="$2"; shift 2 ;;
        --clean) CLEAN=true; shift ;;
        --skip-if-exists) SKIP_IF_EXISTS=true; shift ;;
        *) echo "Unknown: $1"; exit 1 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/app/src/main/jniLibs/arm64-v8a"
OUTPUT_SO="${OUTPUT_DIR}/libVkLayer_LSFGVK_frame_generation.so"

# Skip if .so already exists and --skip-if-exists is set
if [[ "$SKIP_IF_EXISTS" == true ]] && [[ -f "$OUTPUT_SO" ]]; then
    echo "[LSFG] .so already exists at $OUTPUT_SO — skipping build"
    ls -lh "$OUTPUT_SO"
    exit 0
fi

if [[ -z "$NDK" ]]; then
    echo "ERROR: Android NDK not found. Set ANDROID_NDK_HOME or pass --ndk"
    exit 1
fi

BUILD_DIR="${SCRIPT_DIR}/build/lsfg-vk-android"
ABI="arm64-v8a"
API_LEVEL="26"
TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/linux-x86_64"

# Find cmake — try NDK-bundled first, then system
CMAKE="${TOOLCHAIN}/bin/cmake"
if [ ! -f "$CMAKE" ]; then
    CMAKE="$(command -v cmake 2>/dev/null || true)"
fi
if [ -z "$CMAKE" ] || [ ! -f "$CMAKE" ]; then
    echo "ERROR: cmake not found in NDK toolchain or system PATH"
    exit 1
fi

NINJA="${TOOLCHAIN}/bin/ninja"

if [[ "$CLEAN" == true ]]; then
    rm -rf "$BUILD_DIR"
    echo "Cleaned build directory"
fi

mkdir -p "$BUILD_DIR"

echo "=== Building lsfg-vk for Android ARM64 ==="
echo "NDK:      $NDK"
echo "CMake:    $CMAKE"
echo "ABI:      $ABI"
echo "API:      $API_LEVEL"
echo "Build:    $BUILD_DIR"
echo "Output:   $OUTPUT_DIR"

# Configure with CMake
"$CMAKE" -S "${SCRIPT_DIR}/app/src/main/cpp/lsfg-vk" -B "$BUILD_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="${NDK}/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="android-${API_LEVEL}" \
    -DANDROID_STL="c++_shared" \
    -DCMAKE_BUILD_TYPE=Release \
    -DLSFGVK_BUILD_VK_LAYER=ON \
    -DLSFGVK_BUILD_CLI=OFF \
    -DLSFGVK_BUILD_UI=OFF \
    -DFETCHCONTENT_QUIET=OFF

# Build
"$CMAKE" --build "$BUILD_DIR" --parallel "$(nproc 2>/dev/null || echo 4)"

# Copy the resulting .so to jniLibs (search in case path differs)
mkdir -p "$OUTPUT_DIR"
FOUND_SO=$(find "$BUILD_DIR" -name "libVkLayer_LSFGVK_frame_generation.so" -type f 2>/dev/null | head -1)
if [ -n "$FOUND_SO" ]; then
    cp "$FOUND_SO" "$OUTPUT_DIR/"
    echo "=== Done ==="
    echo "Library copied from: $FOUND_SO"
    ls -lh "$OUTPUT_SO"
else
    echo "ERROR: libVkLayer_LSFGVK_frame_generation.so not found in $BUILD_DIR"
    find "$BUILD_DIR" -name "*.so" -type f 2>/dev/null || true
    exit 1
fi

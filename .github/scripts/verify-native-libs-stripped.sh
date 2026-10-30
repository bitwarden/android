#!/usr/bin/env bash
set -euo pipefail

# Fails if any native library packaged for a supported ABI still has its debug symbols.
# AGP's strip*DebugSymbols tasks only log a warning when they cannot strip (for example when the
# NDK they need is missing), so this check keeps unstripped libraries from shipping unnoticed.
# Legacy ABIs (armeabi, mips, mips64) are ignored because current NDKs cannot strip them.
# Usage: verify-native-libs-stripped.sh <apk|aab|directory>...

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <apk|aab|directory>..." >&2
    exit 1
fi

SUPPORTED_ABI_LIB_REGEX='(^|/)lib/(arm64-v8a|armeabi-v7a|x86|x86_64)/[^/]+\.so$'

archives=()
for path in "$@"; do
    if [ -d "$path" ]; then
        while IFS= read -r archive; do
            archives+=("$archive")
        done < <(find "$path" -type f \( -name '*.apk' -o -name '*.aab' \) | sort)
    elif [ -f "$path" ]; then
        archives+=("$path")
    else
        echo "::error::$path does not exist"
        exit 1
    fi
done

if [ "${#archives[@]}" -eq 0 ]; then
    echo "::error::No APK or AAB files found in: $*"
    exit 1
fi

work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT

failures=0
for archive in "${archives[@]}"; do
    libs=$(unzip -Z1 "$archive" | grep -E "$SUPPORTED_ABI_LIB_REGEX" || true)
    if [ -z "$libs" ]; then
        echo "$archive: no native libraries"
        continue
    fi

    extract_dir="$work_dir/$(basename "$archive")"
    mkdir -p "$extract_dir"
    count=0
    while IFS= read -r lib; do
        unzip -q -o "$archive" "$lib" -d "$extract_dir"
        count=$((count + 1))
        if file -b "$extract_dir/$lib" | grep -q "not stripped"; then
            echo "::error::$archive contains an unstripped native library: $lib"
            failures=$((failures + 1))
        fi
    done <<< "$libs"
    echo "$archive: checked $count native libraries"
done

if [ "$failures" -gt 0 ]; then
    echo "::error::Found $failures unstripped native libraries. Check the strip*DebugSymbols output (run with --info) and make sure the NDK set as ndkVersion in gradle/libs.versions.toml is installed."
    exit 1
fi

echo "All native libraries for supported ABIs are stripped."

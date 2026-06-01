#!/bin/bash
# Download Artifacts Script
#
# This script downloads build artifacts from a GitHub Actions run and processes them
# for consistent naming. It requires:
#   - GitHub CLI (gh) to be installed and authenticated
#   - Two arguments:
#     1. Target path where artifacts should be downloaded
#     2. GitHub Actions run ID to download artifacts from
#
# Example usage:
#   ./download-artifacts.sh 2024.10.2 1234567890
#
# The script will:
# 1. Create the target directory if it doesn't exist
# 2. Download all artifacts from the specified GitHub Actions run
# 3. Process the artifacts to have consistent naming based on their folders
# 4. Move artifacts up to the target path

# Check if required arguments are provided
if [ $# -ne 2 ]; then
    echo "Usage: $0 <path> <github_run_id>"
    exit 1
fi

# Store arguments
TARGET_PATH="$1"
GITHUB_RUN_ID="$2"

# Create target directory if it doesn't exist
mkdir -p "$TARGET_PATH"

# Change to target directory
cd "$TARGET_PATH" || exit 1

# Download artifacts using GitHub CLI
echo "Downloading artifacts from GitHub run $GITHUB_RUN_ID..."
gh run download "$GITHUB_RUN_ID"

# Process downloaded artifacts
for dir in */; do
    # Skip if no directories found
    [ -e "$dir" ] || continue

    # Remove trailing slash from directory name
    dirname=${dir%/}

    # First rename all files inside directory with directory name prefix
    for file in "$dir"*; do
        # Skip if no files found
        [ -e "$file" ] || continue

        # Get just the filename without path
        filename=$(basename "$file")
        # Get just the directory name without path
        foldername=$(basename "$dirname")
        # Rename file with directory name prefix
        mv "$file" "$dir${foldername}"
    done

    # Rename directory to avoid collision with files
    mv "$dir" "${dirname}_temp"

    # Move all files up from renamed directory
    mv "${dirname}_temp"/* .

    # Remove empty directory
    rmdir "${dirname}_temp"
done

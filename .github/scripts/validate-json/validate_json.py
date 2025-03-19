#!/usr/bin/env python3
import json
import sys
import os
from typing import List, Dict, Any, Set


def get_package_names(file_path: str) -> Set[str]:
    """
    Extracts package names from a JSON file.

    Args:
        file_path: Path to the JSON file

    Returns:
        Set of package names
    """
    with open(file_path, 'r') as f:
        data = json.load(f)

    package_names = set()
    for app in data["apps"]:
        package_names.add(app["info"]["package_name"])

    return package_names


def validate_json(file_path: str) -> bool:
    """
    Validates if a JSON file is correctly formatted by attempting to deserialize it.

    Args:
        file_path: Path to the JSON file to validate

    Returns:
        True if valid, False otherwise
    """
    try:
        if not os.path.exists(file_path):
            print(f"Error: File {file_path} does not exist")
            return False

        with open(file_path, 'r') as f:
            json.load(f)
        print(f"✅ JSON file {file_path} is valid")
        return True
    except json.JSONDecodeError as e:
        print(f"❌ Invalid JSON in {file_path}: {str(e)}")
        return False
    except Exception as e:
        print(f"❌ Error validating {file_path}: {str(e)}")
        return False


def find_duplicates(file1_path: str, file2_path: str) -> List[str]:
    """
    Checks for duplicate package_name entries between two JSON files.

    Args:
        file1_path: Path to the first JSON file
        file2_path: Path to the second JSON file

    Returns:
        List of duplicate package names, empty list if none found
    """
    try:
        # Get package names from both files
        packages1 = get_package_names(file1_path)
        packages2 = get_package_names(file2_path)

        # Find duplicates
        duplicates = list(packages1.intersection(packages2))

        if duplicates:
            print(f"❌ Found {len(duplicates)} duplicate package names between {file1_path} and {file2_path}:")
            for dup in duplicates:
                print(f"  - {dup}")
            return duplicates
        else:
            print(f"✅ No duplicate package names found between {file1_path} and {file2_path}")
            return []

    except Exception as e:
        print(f"❌ Error checking duplicates: {str(e)}")
        return []


def save_duplicates_to_file(duplicates: List[str], output_file: str) -> None:
    """
    Saves the list of duplicates to a file.

    Args:
        duplicates: List of duplicate package names
        output_file: Path to save the list of duplicates
    """
    try:
        with open(output_file, 'w') as f:
            for dup in duplicates:
                f.write(f"{dup}\n")
        print(f"Duplicates saved to {output_file}")
    except Exception as e:
        print(f"❌ Error saving duplicates to file: {str(e)}")


def main():
    if len(sys.argv) < 2:
        print("Usage:")
        print("  Validate JSON: python validate_json.py validate <json_file>")
        print("  Check duplicates: python validate_json.py duplicates <json_file1> <json_file2> [output_file]")
        sys.exit(1)

    command = sys.argv[1]

    match command:
        case "validate":
            if len(sys.argv) < 3:
                print("Error: Missing JSON file path")
                sys.exit(1)

            file_path = sys.argv[2]
            success = validate_json(file_path)
            sys.exit(0 if success else 1)

        case "duplicates":
            if len(sys.argv) < 4:
                print("Error: Missing JSON file paths")
                sys.exit(1)

            file1_path = sys.argv[2]
            file2_path = sys.argv[3]
            output_file = sys.argv[4] if len(sys.argv) > 4 else "duplicates.txt"

            duplicates = find_duplicates(file1_path, file2_path)
            if duplicates:
                save_duplicates_to_file(duplicates, output_file)

            sys.exit(0)

        case _:
            print(f"Unknown command: {command}")
            sys.exit(1)


if __name__ == "__main__":
    main()

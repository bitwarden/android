# JSON Validation Scripts

Utility scripts for validating JSON files and checking for duplicate package names between Google and Community privileged browser lists.

## Usage

### Validate a JSON file

```bash
python validate_json.py validate <json_file>
```

### Check for duplicates between two JSON files

```bash
python validate_json.py duplicates <json_file1> <json_file2> [output_file]
```

If `output_file` is not specified, duplicates will be saved to `duplicates.txt`.

## Running Tests

```bash
# Run all tests
python -m unittest test_validate_json.py

# Run the invalid JSON test individually
python -m unittest test_validate_json.TestValidateJson.test_validate_json_invalid
```

## Examples

```bash
# Validate Google privileged browsers list
python validate_json.py validate ../../app/src/main/assets/fido2_privileged_google.json

# Check for duplicates between Google and Community lists
python validate_json.py duplicates ../../app/src/main/assets/fido2_privileged_google.json ../../app/src/main/assets/fido2_privileged_community.json duplicates.txt
```

#!/usr/bin/env python3
import unittest
import os
import json
from validate_json import validate_json, find_duplicates, get_package_names
from unittest.mock import patch
import io


class TestValidateJson(unittest.TestCase):
    def setUp(self):
        self.valid_file = os.path.join(os.path.dirname(__file__), "fixtures/sample-valid1.json")
        self.valid_file2 = os.path.join(os.path.dirname(__file__), "fixtures/sample-valid2.json")
        self.invalid_file = os.path.join(os.path.dirname(__file__), "fixtures/sample-invalid.json")

        # Suppress stdout
        self.stdout_patcher = patch('sys.stdout', new=io.StringIO())
        self.stdout_patcher.start()

    def tearDown(self):
        self.stdout_patcher.stop()

    def test_validate_json_valid(self):
        """Test validation of valid JSON file"""
        self.assertTrue(validate_json(self.valid_file))

    def test_validate_json_invalid(self):
        """Test validation of invalid JSON file"""
        self.assertFalse(validate_json(self.invalid_file))

    def test_find_duplicates(self):
        """Test when using the same file (should find duplicates)"""
        expected_package_names = get_package_names(self.valid_file)

        duplicates = find_duplicates(self.valid_file, self.valid_file)

        self.assertEqual(len(duplicates), len(expected_package_names))
        for package_name in expected_package_names:
            self.assertIn(package_name, duplicates)

    def test_find_duplicates_returns_empty_list_when_no_duplicates(self):
        """Test when using different files (should not find duplicates)"""
        duplicates = find_duplicates(self.valid_file, self.valid_file2)
        self.assertEqual(len(duplicates), 0)


if __name__ == "__main__":
    unittest.main()

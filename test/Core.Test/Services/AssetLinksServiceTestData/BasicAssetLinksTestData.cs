public static class BasicAssetLinksTestData
{
    #region Valid statements

    public static string OneStatementOneFingerprintJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.get_login_creds",
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": [
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
                    ]
                }
            }
        ]
        """;
    }

    public static string OneStatementMultipleFingerprintsJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.get_login_creds",
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": [
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:01",
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:02"
                    ]
                }
            }
        ]
        """;
    }

    public static string MultipleStatementsJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.get_login_creds",
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": [
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:01",
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:02"
                    ]
                }
            },
            {
                "relation": [
                    "delegate_permission/common.get_login_creds",
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.foo.app",
                    "sha256_cert_fingerprints": [
                        "10:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
                        "10:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:01",
                        "10:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:02"
                    ]
                }
            }
        ]
        """;
    }

    #endregion

    #region Invalid statements
    
    public static string OneStatementNoGetLoginCredsRelationJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": [
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
                    ]
                }
            }
        ]
        """;
    }
    
    public static string OneStatementNoHandleAllUrlsRelationJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.get_login_creds"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": [
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
                    ]
                }
            }
        ]
        """;
    }
    
    public static string OneStatementWrongNamespaceJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.get_login_creds",
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "NOT_android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": [
                        "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
                    ]
                }
            }
        ]
        """;
    }
    
    public static string OneStatementNoFingerprintsJson()
    {
        return 
        """
        [
            {
                "relation": [
                    "delegate_permission/common.get_login_creds",
                    "delegate_permission/common.handle_all_urls"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": "com.example.app",
                    "sha256_cert_fingerprints": []
                }
            }
        ]
        """;
    }

    #endregion
}
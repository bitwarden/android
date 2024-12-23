package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.vault.datasource.network.api.SyncApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class SyncServiceTest : BaseServiceTest() {
    private val syncApi: SyncApi = retrofit.create()

    private val syncService: SyncService = SyncServiceImpl(
        syncApi = syncApi,
    )

    @Test
    fun `sync should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(SYNC_SUCCESS_JSON))
        val result = syncService.sync()
        assertEquals(createMockSyncResponse(number = 1), result.getOrThrow())
    }

    @Test
    fun `revision date should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(REVISION_DATE_SUCCESS_JSON))
        val result = syncService.getAccountRevisionDateMillis()
        assertEquals(
            REVISION_DATE_MILLISECONDS,
            result.getOrThrow(),
        )
    }
}

private const val REVISION_DATE_MILLISECONDS = 1707847670747L
private const val REVISION_DATE_SUCCESS_JSON = """1707847670747"""

private const val SYNC_SUCCESS_JSON = """
{
  "profile": {
    "id": "mockId-1",
    "name": "mockName-1",
    "email": "mockEmail-1",
    "emailVerified": false,
    "premium": false,
    "premiumFromOrganization": false,
    "masterPasswordHint": "mockMasterPasswordHint-1",
    "culture": "mockCulture-1",
    "twoFactorEnabled": false,
    "key": "mockKey-1",
    "privateKey": "mockPrivateKey-1",
    "securityStamp": "mockSecurityStamp-1",
    "forcePasswordReset": false,
    "usesKeyConnector": false,
    "avatarColor": "mockAvatarColor-1",
    "creationDate": "2024-09-13T01:00:00.00Z",
    "organizations": [
      {
        "usePolicies": false,
        "keyConnectorEnabled": false,
        "keyConnectorUrl": "mockKeyConnectorUrl-1",
        "type": 1,
        "seats": 1,
        "enabled": false,
        "providerType": 1,
        "maxCollections": 1,
        "selfHost": false,
        "permissions": {
          "manageGroups": false,
          "manageResetPassword": false,
          "accessReports": false,
          "managePolicies": false,
          "deleteAnyCollection": false,
          "manageSso": false,
          "deleteAssignedCollections": false,
          "manageUsers": false,
          "accessImportExport": false,
          "editAnyCollection": false,
          "accessEventLogs": false,
          "createNewCollections": false,
          "editAssignedCollections": false
        },
        "providerId": "mockProviderId-1",
        "id": "mockId-1",
        "useGroups": false,
        "useDirectory": false,
        "key": "mockKey-1",
        "providerName": "mockProviderName-1",
        "usersGetPremium": false,
        "maxStorageGb": 1,
        "identifier": "mockIdentifier-1",
        "use2fa": false,
        "familySponsorshipToDelete": false,
        "userId": "mockUserId-1",
        "useEvents": false,
        "familySponsorshipFriendlyName": "mockFamilySponsorshipFriendlyName-1",
        "useTotp": false,
        "familySponsorshipLastSyncDate": "2023-10-27T12:00:00.00Z",
        "name": "mockName-1",
        "useApi": false,
        "familySponsorshipValidUntil": "2023-10-27T12:00:00.00Z",  
        "status": 1
      }
    ],
    "providers": [
      {
        "useEvents": false,
        "permissions": {
          "manageGroups": false,
          "manageResetPassword": false,
          "accessReports": false,
          "managePolicies": false,
          "deleteAnyCollection": false,
          "manageSso": false,
          "deleteAssignedCollections": false,
          "manageUsers": false,
          "accessImportExport": false,
          "editAnyCollection": false,
          "accessEventLogs": false,
          "createNewCollections": false,
          "editAssignedCollections": false
        },
        "name": "mockName-1",
        "id": "mockId-1",
        "type": 1,
        "userId": "mockUserId-1",
        "key": "mockKey-1",
        "enabled": false,
        "status": 1
      }
    ],
    "providerOrganizations": [
      {
        "usePolicies": false,
        "keyConnectorEnabled": false,
        "keyConnectorUrl": "mockKeyConnectorUrl-1",
        "type": 1,
        "seats": 1,
        "enabled": false,
        "providerType": 1,
        "maxCollections": 1,
        "selfHost": false,
        "permissions": {
          "manageGroups": false,
          "manageResetPassword": false,
          "accessReports": false,
          "managePolicies": false,
          "deleteAnyCollection": false,
          "manageSso": false,
          "deleteAssignedCollections": false,
          "manageUsers": false,
          "accessImportExport": false,
          "editAnyCollection": false,
          "accessEventLogs": false,
          "createNewCollections": false,
          "editAssignedCollections": false
        },
        "providerId": "mockProviderId-1",
        "id": "mockId-1",
        "useGroups": false,
        "useDirectory": false,
        "key": "mockKey-1",
        "providerName": "mockProviderName-1",
        "usersGetPremium": false,
        "maxStorageGb": 1,
        "identifier": "mockIdentifier-1",
        "use2fa": false,
        "familySponsorshipToDelete": false,
        "userId": "mockUserId-1",
        "useEvents": false,
        "familySponsorshipFriendlyName": "mockFamilySponsorshipFriendlyName-1",
        "useTotp": false,
        "familySponsorshipLastSyncDate": "2023-10-27T12:00:00.00Z",
        "name": "mockName-1",
        "useApi": false,
        "familySponsorshipValidUntil": "2023-10-27T12:00:00.00Z",  
        "status": 1
      }
    ]
  },
  "folders": [
    {
      "revisionDate": "2023-10-27T12:00:00.00Z",
      "name": "mockName-1",
      "id": "mockId-1"
    }
  ],
  "collections": [
    {
      "organizationId": "mockOrganizationId-1",
      "hidePasswords": false,
      "name": "mockName-1",
      "externalId": "mockExternalId-1",
      "readOnly": false,
      "id": "mockId-1",
      "manage": true
    }
  ],
  "ciphers": [
    {
      "notes": "mockNotes-1",
      "attachments": [
        {
          "fileName": "mockFileName-1",
          "size": 1,
          "sizeName": "mockSizeName-1",
          "id": "mockId-1",
          "url": "mockUrl-1",
          "key": "mockKey-1"
        }
      ],
      "organizationUseTotp": false,
      "reprompt": 0,
      "edit": false,
      "passwordHistory": [
        {
          "password": "mockPassword-1",
          "lastUsedDate": "2023-10-27T12:00:00.00Z"
        }
      ],
      "revisionDate": "2023-10-27T12:00:00.00Z",
      "type": 1,
      "login": {
        "uris": [
          {
            "match": 1,
            "uri": "mockUri-1",
            "uriChecksum": "mockUriChecksum-1"
          }
        ],
        "totp": "mockTotp-1",
        "password": "mockPassword-1",
        "passwordRevisionDate": "2023-10-27T12:00:00.00Z",
        "autofillOnPageLoad": false,
        "uri": "mockUri-1",
        "username": "mockUsername-1",
        "fido2Credentials": [
          {
            "credentialId": "mockCredentialId-1",
            "keyType": "mockKeyType-1",
            "keyAlgorithm": "mockKeyAlgorithm-1",
            "keyCurve": "mockKeyCurve-1",
            "keyValue": "mockKeyValue-1",
            "rpId": "mockRpId-1",
            "rpName": "mockRpName-1",
            "userHandle": "mockUserHandle-1",
            "userName": "mockUserName-1",
            "userDisplayName": "mockUserDisplayName-1",
            "counter": "mockCounter-1",
            "discoverable": "mockDiscoverable-1",
            "creationDate": "2023-10-27T12:00:00.00Z"
          }
        ]
      },
      "creationDate": "2023-10-27T12:00:00.00Z",
      "secureNote": {
        "type": 0
      },
      "folderId": "mockFolderId-1",
      "organizationId": "mockOrganizationId-1",
      "deletedDate": "2023-10-27T12:00:00.00Z",
      "identity": {
        "passportNumber": "mockPassportNumber-1",
        "lastName": "mockLastName-1",
        "address3": "mockAddress3-1",
        "address2": "mockAddress2-1",
        "city": "mockCity-1",
        "country": "mockCountry-1",
        "address1": "mockAddress1-1",
        "postalCode": "mockPostalCode-1",
        "title": "mockTitle-1",
        "ssn": "mockSsn-1",
        "firstName": "mockFirstName-1",
        "phone": "mockPhone-1",
        "middleName": "mockMiddleName-1",
        "company": "mockCompany-1",
        "licenseNumber": "mockLicenseNumber-1",
        "state": "mockState-1",
        "email": "mockEmail-1",
        "username": "mockUsername-1"
      },
      "collectionIds": [
        "mockCollectionId-1"
      ],
      "name": "mockName-1",
      "id": "mockId-1"
      "fields": [
        {
          "linkedId": 100,
          "name": "mockName-1",
          "type": 1,
          "value": "mockValue-1"
        }
      ],
      "viewPassword": false,
      "favorite": false,
      "card": {
        "number": "mockNumber-1",
        "expMonth": "mockExpMonth-1",
        "code": "mockCode-1",
        "expYear": "mockExpirationYear-1",
        "cardholderName": "mockCardholderName-1",
        "brand": "mockBrand-1"
      },
      "key": "mockKey-1",
      "sshKey": {
        "publicKey": "mockPublicKey-1",
        "privateKey": "mockPrivateKey-1",
        "keyFingerprint": "mockKeyFingerprint-1"
      }
    }
  ],
  "domains": {
    "equivalentDomains": [
      [
        "mockEquivalentDomain-1"
      ]
    ],
    "globalEquivalentDomains": [
       {
        "type": 1,
        "domains": [
          "mockDomain-1"
        ],
        "excluded": false
      }
    ]
  },
  "policies": [
    {
      "organizationId": "mockOrganizationId-1",
      "id": "mockId-1",
      "type": 1,
      "enabled": false
    }
  ],
  "sends": [
    {
      "accessCount": 1,
      "notes": "mockNotes-1",
      "revisionDate": "2023-10-27T12:00:00.00Z",
      "maxAccessCount": 1,
      "hideEmail": false,
      "type": 1,
      "accessId": "mockAccessId-1",
      "password": "mockPassword-1",
      "file": {
        "fileName": "mockFileName-1",
        "size": 1,
        "sizeName": "mockSizeName-1",
        "id": "mockId-1"
      },
      "deletionDate": "2023-10-27T12:00:00.00Z",
      "name": "mockName-1",
      "disabled": false,
      "id": "mockId-1",
      "text": {
        "hidden": false,
        "text": "mockText-1"
      },
      "key": "mockKey-1",
      "expirationDate": "2023-10-27T12:00:00.00Z"
    }
  ]
}    
"""

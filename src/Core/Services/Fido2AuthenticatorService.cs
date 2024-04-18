using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Enums;
using Bit.Core.Utilities.Fido2;
using Bit.Core.Utilities;
using System.Formats.Cbor;
using System.Security.Cryptography;

namespace Bit.Core.Services
{
    public class Fido2AuthenticatorService : IFido2AuthenticatorService
    {
        // AAGUID: d548826e-79b4-db40-a3d8-11116f7e8349
        public static readonly byte[] AAGUID = new byte[] { 0xd5, 0x48, 0x82, 0x6e, 0x79, 0xb4, 0xdb, 0x40, 0xa3, 0xd8, 0x11, 0x11, 0x6f, 0x7e, 0x83, 0x49 };

        private readonly ICipherService _cipherService;
        private readonly ISyncService _syncService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;

        public Fido2AuthenticatorService(ICipherService cipherService,
            ISyncService syncService,
            ICryptoFunctionService cryptoFunctionService,
            IUserVerificationMediatorService userVerificationMediatorService)
        {
            _cipherService = cipherService;
            _syncService = syncService;
            _cryptoFunctionService = cryptoFunctionService;
            _userVerificationMediatorService = userVerificationMediatorService;
        }

        public async Task<Fido2AuthenticatorMakeCredentialResult> MakeCredentialAsync(Fido2AuthenticatorMakeCredentialParams makeCredentialParams, IFido2MakeCredentialUserInterface userInterface)
        {
            if (makeCredentialParams.CredTypesAndPubKeyAlgs.All((p) => p.Alg != (int)Fido2AlgorithmIdentifier.ES256))
            {
                throw new NotSupportedError();
            }

            string cipherId = null;
            var userVerified = false;
            var accountSwitched = false;

            do
            {
                try
                {
                    accountSwitched = false;

                    await userInterface.EnsureUnlockedVaultAsync();
                    await _syncService.FullSyncAsync(false);

                    var existingCipherIds = await FindExcludedCredentialsAsync(
                        makeCredentialParams.ExcludeCredentialDescriptorList
                    );
                    if (existingCipherIds.Length > 0)
                    {
                        await userInterface.InformExcludedCredentialAsync(existingCipherIds);
                        throw new NotAllowedError();
                    }

                    (cipherId, userVerified) = await userInterface.ConfirmNewCredentialAsync(new Fido2ConfirmNewCredentialParams
                    {
                        CredentialName = makeCredentialParams.RpEntity.Name,
                        UserName = makeCredentialParams.UserEntity.Name,
                        UserVerificationPreference = makeCredentialParams.UserVerificationPreference,
                        RpId = makeCredentialParams.RpEntity.Id
                    });
                }
                catch (AccountSwitchedException)
                {
                    accountSwitched = true;
                }
            } while (accountSwitched);

            string credentialId;
            if (cipherId == null)
            {
                throw new NotAllowedError();
            }

            try
            {
                var keyPair = GenerateKeyPair();
                var fido2Credential = CreateCredentialView(makeCredentialParams, keyPair.privateKey);

                var encrypted = await _cipherService.GetAsync(cipherId);
                var cipher = await encrypted.DecryptAsync();

                if (!userVerified
                    &&
                    await _userVerificationMediatorService.ShouldEnforceFido2RequiredUserVerificationAsync(new Fido2UserVerificationOptions(
                        cipher.Reprompt != CipherRepromptType.None,
                        makeCredentialParams.UserVerificationPreference,
                        userInterface.HasVaultBeenUnlockedInThisTransaction)))
                {
                    throw new NotAllowedError();
                }

                cipher.Login.Fido2Credentials = new List<Fido2CredentialView> { fido2Credential };
                var reencrypted = await _cipherService.EncryptAsync(cipher);
                await _cipherService.SaveWithServerAsync(reencrypted);
                credentialId = fido2Credential.CredentialId;

                var authData = await GenerateAuthDataAsync(
                    rpId: makeCredentialParams.RpEntity.Id,
                    counter: fido2Credential.CounterValue,
                    userPresence: true,
                    userVerification: userVerified,
                    credentialId: credentialId.GuidToRawFormat(),
                    publicKey: keyPair.publicKey
                );

                return new Fido2AuthenticatorMakeCredentialResult
                {
                    CredentialId = credentialId.GuidToRawFormat(),
                    AttestationObject = EncodeAttestationObject(authData),
                    AuthData = authData,
                    PublicKey = keyPair.publicKey.ExportDer(),
                    PublicKeyAlgorithm = (int)Fido2AlgorithmIdentifier.ES256,
                };
            }
            catch (NotAllowedError)
            {
                throw;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw new UnknownError();
            }
        }

        public async Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams, IFido2GetAssertionUserInterface userInterface)
        {
            List<CipherView> cipherOptions = new List<CipherView>();

            string selectedCipherId = null;
            var userVerified = false;
            var accountSwitched = false;

            do
            {
                try
                {
                    accountSwitched = false;

                    await userInterface.EnsureUnlockedVaultAsync();
                    await _syncService.FullSyncAsync(false);

                    if (assertionParams.AllowCredentialDescriptorList?.Length > 0)
                    {
                        cipherOptions = await FindCredentialsByIdAsync(
                            assertionParams.AllowCredentialDescriptorList,
                            assertionParams.RpId
                        );
                    }
                    else
                    {
                        cipherOptions = await FindCredentialsByRpAsync(assertionParams.RpId);
                    }

                    if (cipherOptions.Count == 0)
                    {
                        throw new NotAllowedError();
                    }

                    (selectedCipherId, userVerified) = await userInterface.PickCredentialAsync(
                        cipherOptions.Select((cipher) => new Fido2GetAssertionUserInterfaceCredential
                        {
                            CipherId = cipher.Id,
                            UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.GetUserVerificationPreferenceFrom(assertionParams.UserVerificationPreference, cipher.Reprompt)
                        }).ToArray()
                    );

                }
                catch (AccountSwitchedException)
                {
                    accountSwitched = true;
                }
            } while (accountSwitched);

            var selectedCipher = cipherOptions.FirstOrDefault((c) => c.Id == selectedCipherId);
            if (selectedCipher == null)
            {
                throw new NotAllowedError();
            }

            if (!userVerified
                &&
                await _userVerificationMediatorService.ShouldEnforceFido2RequiredUserVerificationAsync(new Fido2UserVerificationOptions(
                    selectedCipher.Reprompt != CipherRepromptType.None,
                    assertionParams.UserVerificationPreference,
                    userInterface.HasVaultBeenUnlockedInThisTransaction)))
            {
                throw new NotAllowedError();
            }

            try
            {
                var selectedFido2Credential = selectedCipher.Login.MainFido2Credential;
                var selectedCredentialId = selectedFido2Credential.CredentialId;

                await _cipherService.UpdateLastUsedDateAsync(selectedCipher.Id);

                if (selectedFido2Credential.CounterValue != 0)
                {
                    ++selectedFido2Credential.CounterValue;
                    var encrypted = await _cipherService.EncryptAsync(selectedCipher);
                    await _cipherService.SaveWithServerAsync(encrypted);
                }

                var authenticatorData = await GenerateAuthDataAsync(
                    rpId: selectedFido2Credential.RpId,
                    userPresence: true,
                    userVerification: userVerified,
                    counter: selectedFido2Credential.CounterValue
                );

                var signature = GenerateSignature(
                    authData: authenticatorData,
                    clientDataHash: assertionParams.Hash,
                    privateKey: selectedFido2Credential.KeyBytes
                );

                return new Fido2AuthenticatorGetAssertionResult
                {
                    SelectedCredential = new Fido2SelectedCredential
                    {
                        Id = selectedCredentialId.GuidToRawFormat(),
                        UserHandle = selectedFido2Credential.UserHandleValue,
                        Cipher = selectedCipher
                    },
                    AuthenticatorData = authenticatorData,
                    Signature = signature
                };
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw new UnknownError();
            }
        }

        public async Task<Fido2AuthenticatorDiscoverableCredentialMetadata[]> SilentCredentialDiscoveryAsync(string rpId)
        {
            var credentials = (await FindCredentialsByRpAsync(rpId)).Select(cipher => new Fido2AuthenticatorDiscoverableCredentialMetadata
            {
                Type = Constants.DefaultFido2CredentialType,
                Id = cipher.Login.MainFido2Credential.CredentialId.GuidToRawFormat(),
                RpId = cipher.Login.MainFido2Credential.RpId,
                UserHandle = cipher.Login.MainFido2Credential.UserHandleValue,
                UserName = cipher.Login.MainFido2Credential.UserName,
                CipherId = cipher.Id,
            }).ToArray();

            return credentials;
        }

        /// <summary>
        /// Finds existing crendetials and returns the `CipherId` for each one
        /// </summary>
        private async Task<string[]> FindExcludedCredentialsAsync(
            PublicKeyCredentialDescriptor[] credentials
        )
        {
            if (credentials == null || credentials.Length == 0)
            {
                return Array.Empty<string>();
            }

            var ids = new List<string>();

            foreach (var credential in credentials)
            {
                try
                {
                    ids.Add(credential.Id.GuidToStandardFormat());
                }
                catch { }
            }

            if (ids.Count == 0)
            {
                return Array.Empty<string>();
            }

            var ciphers = await _cipherService.GetAllDecryptedAsync();
            return ciphers
                .FindAll(
                    (cipher) =>
                    !cipher.IsDeleted &&
                    cipher.OrganizationId == null &&
                    cipher.Type == CipherType.Login &&
                    cipher.Login.HasFido2Credentials &&
                    ids.Contains(cipher.Login.MainFido2Credential.CredentialId)
                )
                .Select((cipher) => cipher.Id)
                .ToArray();
        }

        private async Task<List<CipherView>> FindCredentialsByIdAsync(PublicKeyCredentialDescriptor[] credentials, string rpId)
        {
            var ids = new List<string>();

            foreach (var credential in credentials)
            {
                try
                {
                    ids.Add(credential.Id.GuidToStandardFormat());
                }
                catch { }
            }

            if (ids.Count == 0)
            {
                return new List<CipherView>();
            }

            var ciphers = await _cipherService.GetAllDecryptedAsync();
            return ciphers.FindAll((cipher) =>
                !cipher.IsDeleted &&
                cipher.Type == CipherType.Login &&
                cipher.Login.HasFido2Credentials &&
                cipher.Login.MainFido2Credential.RpId == rpId &&
                ids.Contains(cipher.Login.MainFido2Credential.CredentialId)
            );
        }

        private async Task<List<CipherView>> FindCredentialsByRpAsync(string rpId)
        {
            var ciphers = await _cipherService.GetAllDecryptedAsync();
            return ciphers.FindAll((cipher) =>
                !cipher.IsDeleted &&
                cipher.Type == CipherType.Login &&
                cipher.Login.HasFido2Credentials &&
                cipher.Login.MainFido2Credential.RpId == rpId &&
                cipher.Login.MainFido2Credential.DiscoverableValue
            );
        }

        // TODO: Move this to a separate service
        private (PublicKey publicKey, byte[] privateKey) GenerateKeyPair()
        {
            var dsa = ECDsa.Create();
            dsa.GenerateKey(ECCurve.NamedCurves.nistP256);
            var privateKey = dsa.ExportPkcs8PrivateKey();

            return (new PublicKey(dsa), privateKey);
        }

        private Fido2CredentialView CreateCredentialView(Fido2AuthenticatorMakeCredentialParams makeCredentialsParams, byte[] privateKey)
        {
            return new Fido2CredentialView
            {
                CredentialId = Guid.NewGuid().ToString(),
                KeyType = Constants.DefaultFido2CredentialType,
                KeyAlgorithm = Constants.DefaultFido2CredentialAlgorithm,
                KeyCurve = Constants.DefaultFido2CredentialCurve,
                KeyValue = CoreHelpers.Base64UrlEncode(privateKey),
                RpId = makeCredentialsParams.RpEntity.Id,
                UserHandle = CoreHelpers.Base64UrlEncode(makeCredentialsParams.UserEntity.Id),
                UserName = makeCredentialsParams.UserEntity.Name,
                CounterValue = 0,
                RpName = makeCredentialsParams.RpEntity.Name,
                UserDisplayName = makeCredentialsParams.UserEntity.DisplayName,
                DiscoverableValue = makeCredentialsParams.RequireResidentKey,
                CreationDate = DateTime.UtcNow
            };
        }

        private async Task<byte[]> GenerateAuthDataAsync(
            string rpId,
            bool userVerification,
            bool userPresence,
            int counter,
            byte[] credentialId = null,
            PublicKey publicKey = null
        )
        {
            var isAttestation = credentialId != null && publicKey != null;

            List<byte> authData = new List<byte>();

            var rpIdHash = await _cryptoFunctionService.HashAsync(rpId, CryptoHashAlgorithm.Sha256);
            authData.AddRange(rpIdHash);

            var flags = AuthDataFlags(
                extensionData: false,
                attestationData: isAttestation,
                userVerification: userVerification,
                userPresence: userPresence
            );
            authData.Add(flags);

            authData.AddRange(new List<byte> {
                (byte)(counter >> 24),
                (byte)(counter >> 16),
                (byte)(counter >> 8),
                (byte)counter
            });

            if (isAttestation)
            {
                var attestedCredentialData = new List<byte>();

                attestedCredentialData.AddRange(AAGUID);

                // credentialIdLength (2 bytes) and credential Id
                var credentialIdLength = new byte[] {
                    (byte)((credentialId.Length - (credentialId.Length & 0xff)) / 256),
                    (byte)(credentialId.Length & 0xff)
                };
                attestedCredentialData.AddRange(credentialIdLength);
                attestedCredentialData.AddRange(credentialId);
                attestedCredentialData.AddRange(publicKey.ExportCose());

                authData.AddRange(attestedCredentialData);
            }

            return authData.ToArray();
        }

        private byte AuthDataFlags(bool extensionData, bool attestationData, bool userVerification, bool userPresence, bool backupEligibility = true, bool backupState = true)
        {
            byte flags = 0;

            if (extensionData)
            {
                flags |= 0b1000000;
            }

            if (attestationData)
            {
                flags |= 0b01000000;
            }

            if (backupState)
            {
                flags |= 0b00010000;
            }

            if (backupEligibility)
            {
                flags |= 0b00001000;
            }

            if (userVerification)
            {
                flags |= 0b00000100;
            }

            if (userPresence)
            {
                flags |= 0b00000001;
            }

            return flags;
        }

        private byte[] EncodeAttestationObject(byte[] authData)
        {
            var attestationObject = new CborWriter(CborConformanceMode.Ctap2Canonical);
            attestationObject.WriteStartMap(3);
            attestationObject.WriteTextString("fmt");
            attestationObject.WriteTextString("none");
            attestationObject.WriteTextString("attStmt");
            attestationObject.WriteStartMap(0);
            attestationObject.WriteEndMap();
            attestationObject.WriteTextString("authData");
            attestationObject.WriteByteString(authData);
            attestationObject.WriteEndMap();

            return attestationObject.Encode();
        }

        // TODO: Move this to a separate service
        private byte[] GenerateSignature(byte[] authData, byte[] clientDataHash, byte[] privateKey)
        {
            var sigBase = authData.Concat(clientDataHash).ToArray();
            var dsa = ECDsa.Create();
            dsa.ImportPkcs8PrivateKey(privateKey, out var bytesRead);

            if (bytesRead == 0)
            {
                throw new Exception("Failed to import private key");
            }

            return dsa.SignData(sigBase, HashAlgorithmName.SHA256, DSASignatureFormat.Rfc3279DerSequence);
        }

        private class PublicKey
        {
            private readonly ECDsa _dsa;

            public PublicKey(ECDsa dsa)
            {
                _dsa = dsa;
            }

            public byte[] X => _dsa.ExportParameters(false).Q.X;
            public byte[] Y => _dsa.ExportParameters(false).Q.Y;

            public byte[] ExportDer()
            {
                return _dsa.ExportSubjectPublicKeyInfo();
            }

            public byte[] ExportCose()
            {
                var result = new CborWriter(CborConformanceMode.Ctap2Canonical);
                result.WriteStartMap(5);

                // kty = EC2
                result.WriteInt32(1);
                result.WriteInt32(2);

                // alg = ES256
                result.WriteInt32(3);
                result.WriteInt32((int)Fido2AlgorithmIdentifier.ES256);

                // crv = P-256
                result.WriteInt32(-1);
                result.WriteInt32(1);

                // x
                result.WriteInt32(-2);
                result.WriteByteString(X);

                // y
                result.WriteInt32(-3);
                result.WriteByteString(Y);

                result.WriteEndMap();

                return result.Encode();
            }
        }
    }
}

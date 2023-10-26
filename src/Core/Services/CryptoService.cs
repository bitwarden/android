using System;
using System.Collections.Generic;
using System.Linq;
using System.Numerics;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class CryptoService : ICryptoService
    {
        private const string RANDOM_STRING_CHARSET = "abcdefghijklmnopqrstuvwxyz1234567890";

        private readonly IStateService _stateService;
        private readonly ICryptoFunctionService _cryptoFunctionService;

        private SymmetricCryptoKey _legacyEtmKey;
        private string _masterKeyHash;
        private byte[] _publicKey;
        private byte[] _privateKey;
        private Dictionary<string, OrgKey> _orgKeys;
        private Task<Dictionary<string, OrgKey>> _getOrgKeysTask;

        public CryptoService(
            IStateService stateService,
            ICryptoFunctionService cryptoFunctionService)
        {
            _stateService = stateService;
            _cryptoFunctionService = cryptoFunctionService;
        }

        public void ClearCache()
        {
            _legacyEtmKey = null;
            _masterKeyHash = null;
            _publicKey = null;
            _privateKey = null;
            _orgKeys = null;
        }

        public async Task RefreshKeysAsync()
        {
            // Refresh or clear additional keys such as
            // pin and auto unlock keys
            await SetUserKeyAsync(await GetUserKeyAsync());
        }

        public async Task SetUserKeyAsync(UserKey userKey, string userId = null)
        {
            await _stateService.SetUserKeyAsync(userKey, userId);
            await StoreAdditionalKeysAsync(userKey, userId);
        }

        public Task<UserKey> GetUserKeyAsync(string userId = null)
        {
            return _stateService.GetUserKeyAsync(userId);
        }

        public async Task<bool> IsLegacyUserAsync(MasterKey masterKey = null, string userId = null)
        {
            masterKey ??= await GetMasterKeyAsync(userId);
            if (masterKey == null)
            {
                return false;
            }
            return await ValidateUserKeyAsync(new UserKey(masterKey.Key));
        }

        public async Task<UserKey> GetUserKeyWithLegacySupportAsync(string userId = null)
        {
            var userKey = await GetUserKeyAsync(userId);
            if (userKey != null)
            {
                return userKey;
            }

            // Legacy support: encryption used to be done with the master key (derived from master password).
            // Users who have not migrated will have a null user key and must use the master key instead.
            return new UserKey((await GetMasterKeyAsync(userId)).Key);
        }

        public async Task<bool> HasUserKeyAsync(string userId = null)
        {
            return await GetUserKeyAsync(userId) != null;
        }

        public async Task<bool> HasEncryptedUserKeyAsync(string userId = null)
        {
            return await _stateService.GetMasterKeyEncryptedUserKeyAsync(userId) != null;
        }

        public async Task<UserKey> MakeUserKeyAsync()
        {
            return new UserKey(await _cryptoFunctionService.RandomBytesAsync(64));
        }

        public Task ClearUserKeyAsync(string userId = null)
        {
            return _stateService.SetUserKeyAsync(null, userId);
        }

        public Task SetMasterKeyEncryptedUserKeyAsync(string value, string userId = null)
        {
            return _stateService.SetMasterKeyEncryptedUserKeyAsync(value, userId);
        }

        public async Task<UserKey> GetAutoUnlockKeyAsync(string userId = null)
        {
            await MigrateAutoAndBioKeysIfNeededAsync(userId);
            return await _stateService.GetUserKeyAutoUnlockAsync(userId);
        }

        public async Task<bool> HasAutoUnlockKeyAsync(string userId = null)
        {
            return await GetAutoUnlockKeyAsync(userId) != null;
        }

        public async Task<UserKey> GetBiometricUnlockKeyAsync(string userId = null)
        {
            await MigrateAutoAndBioKeysIfNeededAsync(userId);
            return await _stateService.GetUserKeyBiometricUnlockAsync(userId);
        }

        public Task SetMasterKeyAsync(MasterKey masterKey, string userId = null)
        {
            return _stateService.SetMasterKeyAsync(masterKey, userId);
        }

        public async Task<MasterKey> GetMasterKeyAsync(string userId = null)
        {
            var masterKey = await _stateService.GetMasterKeyAsync(userId);
            if (masterKey == null)
            {
                var masterKeyDecrypted = await _stateService.GetKeyDecryptedAsync(userId);
                if (masterKeyDecrypted == null)
                {
                    return null;
                }

                // Migration support
                masterKey = new MasterKey(masterKeyDecrypted.Key);
                if (masterKey != null)
                {
                    await SetMasterKeyAsync(masterKey, userId);
                }
            }
            return masterKey;
        }

        public Task<MasterKey> MakeMasterKeyAsync(string password, string email, KdfConfig kdfConfig)
        {
            return MakeKeyAsync(password, email, kdfConfig, keyBytes => new MasterKey(keyBytes));
        }

        public Task ClearMasterKeyAsync(string userId = null)
        {
            return _stateService.SetMasterKeyAsync(null, userId);
        }

        public async Task<Tuple<UserKey, EncString>> EncryptUserKeyWithMasterKeyAsync(MasterKey masterKey, UserKey userKey = null)
        {
            userKey ??= await GetUserKeyAsync();
            if (userKey == null)
            {
                throw new UserKeyNullException();
            }
            return await BuildProtectedSymmetricKeyAsync(masterKey, userKey.Key, keyBytes => new UserKey(keyBytes));
        }

        public async Task<UserKey> DecryptUserKeyWithMasterKeyAsync(MasterKey masterKey, EncString encUserKey = null, string userId = null)
        {
            masterKey ??= await GetMasterKeyAsync(userId);
            if (masterKey == null)
            {
                throw new MasterKeyNullException();
            }

            if (encUserKey == null)
            {
                var userKeyMasterKey = await _stateService.GetMasterKeyEncryptedUserKeyAsync(userId);

                if (userKeyMasterKey is null)
                {
                    // Migrate old key
                    var oldEncUserKey = await _stateService.GetEncKeyEncryptedAsync(userId);

                    if (oldEncUserKey is null)
                    {
                        throw new Exception("No encrypted user key nor old encKeyEncrypted found");
                    }

                    var userKey = await DecryptUserKeyWithMasterKeyAsync(
                        masterKey,
                        new EncString(oldEncUserKey),
                        userId
                    );
                    await SetMasterKeyEncryptedUserKeyAsync(oldEncUserKey, userId);
                    await _stateService.SetEncKeyEncryptedAsync(null, userId);
                    return userKey;
                }

                encUserKey = new EncString(userKeyMasterKey);
            }

            byte[] decUserKey = null;
            if (encUserKey.EncryptionType == EncryptionType.AesCbc256_B64)
            {
                decUserKey = await DecryptToBytesAsync(encUserKey, masterKey);
            }
            else if (encUserKey.EncryptionType == EncryptionType.AesCbc256_HmacSha256_B64)
            {
                var newKey = await StretchKeyAsync(masterKey, keyBytes => new MasterKey(keyBytes));
                decUserKey = await DecryptToBytesAsync(encUserKey, newKey);
            }
            else
            {
                throw new Exception($"Unsupported encrypted user key type: {encUserKey.EncryptionType}");
            }

            if (decUserKey == null)
            {
                return null;
            }
            return new UserKey(decUserKey);
        }

        public async Task<Tuple<SymmetricCryptoKey, EncString>> MakeDataEncKeyAsync(SymmetricCryptoKey key)
        {
            if (key is null)
            {
                throw new ArgumentNullException(nameof(key));
            }
            if (!(key is UserKey) && !(key is OrgKey) && !(key is CipherKey))
            {
                throw new ArgumentException($"Data encryption keys must be of type UserKey or OrgKey or CipherKey. {key.GetType().FullName} unsupported.");
            }

            var newSymKey = await _cryptoFunctionService.RandomBytesAsync(64);
            return await BuildProtectedSymmetricKeyAsync(key, newSymKey, keyBytes => new SymmetricCryptoKey(keyBytes));
        }

        public async Task<string> HashMasterKeyAsync(string password, MasterKey masterKey, HashPurpose hashPurpose = HashPurpose.ServerAuthorization)
        {
            if (password is null)
            {
                throw new ArgumentNullException(nameof(password));
            }

            if (masterKey is null)
            {
                masterKey = await GetMasterKeyAsync();

                if (masterKey is null)
                {
                    throw new ArgumentNullException(nameof(masterKey));
                }
            }
            var hash = await _cryptoFunctionService.Pbkdf2Async(masterKey.Key, password, CryptoHashAlgorithm.Sha256, (int)hashPurpose);
            return Convert.ToBase64String(hash);
        }

        public Task SetMasterKeyHashAsync(string keyHash)
        {
            _masterKeyHash = keyHash;
            return _stateService.SetKeyHashAsync(keyHash);
        }

        public async Task<string> GetMasterKeyHashAsync()
        {
            if (_masterKeyHash != null)
            {
                return _masterKeyHash;
            }
            var passwordHash = await _stateService.GetKeyHashAsync();
            if (passwordHash != null)
            {
                _masterKeyHash = passwordHash;
            }
            return _masterKeyHash;
        }

        public Task ClearMasterKeyHashAsync(string userId = null)
        {
            _masterKeyHash = null;
            return _stateService.SetKeyHashAsync(null, userId);
        }

        public async Task<bool> CompareAndUpdateKeyHashAsync(string masterPassword, MasterKey key)
        {
            var storedPasswordHash = await GetMasterKeyHashAsync();
            if (masterPassword != null && storedPasswordHash != null)
            {
                var localPasswordHash = await HashMasterKeyAsync(masterPassword, key, HashPurpose.LocalAuthorization);
                if (localPasswordHash != null && storedPasswordHash == localPasswordHash)
                {
                    return true;
                }

                var serverPasswordHash = await HashMasterKeyAsync(masterPassword, key, HashPurpose.ServerAuthorization);
                if (serverPasswordHash != null && storedPasswordHash == serverPasswordHash)
                {
                    await SetMasterKeyHashAsync(localPasswordHash);
                    return true;
                }
            }

            return false;
        }

        public Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs)
        {
            var orgKeys = orgs.ToDictionary(org => org.Id, org => org.Key);
            _orgKeys = null;
            return _stateService.SetOrgKeysEncryptedAsync(orgKeys);
        }

        public async Task<OrgKey> GetOrgKeyAsync(string orgId)
        {
            if (string.IsNullOrWhiteSpace(orgId))
            {
                return null;
            }
            var orgKeys = await GetOrgKeysAsync();
            if (orgKeys == null || !orgKeys.ContainsKey(orgId))
            {
                return null;
            }
            return orgKeys[orgId];
        }

        public Task<Dictionary<string, OrgKey>> GetOrgKeysAsync()
        {
            if (_orgKeys != null && _orgKeys.Count > 0)
            {
                return Task.FromResult(_orgKeys);
            }
            if (_getOrgKeysTask != null && !_getOrgKeysTask.IsCompleted && !_getOrgKeysTask.IsFaulted)
            {
                return _getOrgKeysTask;
            }
            async Task<Dictionary<string, OrgKey>> doTask()
            {
                try
                {
                    var encOrgKeys = await _stateService.GetOrgKeysEncryptedAsync();
                    if (encOrgKeys == null)
                    {
                        return null;
                    }
                    var orgKeys = new Dictionary<string, OrgKey>();
                    var setKey = false;
                    foreach (var org in encOrgKeys)
                    {
                        var decValue = await RsaDecryptAsync(org.Value);
                        orgKeys.Add(org.Key, new OrgKey(decValue));
                        setKey = true;
                    }

                    if (setKey)
                    {
                        _orgKeys = orgKeys;
                    }
                    return _orgKeys;
                }
                finally
                {
                    _getOrgKeysTask = null;
                }
            }
            _getOrgKeysTask = doTask();
            return _getOrgKeysTask;
        }


        public async Task ClearOrgKeysAsync(bool memoryOnly = false, string userId = null)
        {
            _orgKeys = null;
            if (!memoryOnly)
            {
                await _stateService.SetOrgKeysEncryptedAsync(null, userId);
            }
        }

        public async Task<byte[]> GetUserPublicKeyAsync()
        {
            if (_publicKey != null)
            {
                return _publicKey;
            }
            var privateKey = await GetUserPrivateKeyAsync();
            if (privateKey == null)
            {
                return null;
            }
            _publicKey = await _cryptoFunctionService.RsaExtractPublicKeyAsync(privateKey);
            return _publicKey;
        }

        public async Task SetUserPrivateKeyAsync(string encPrivateKey)
        {
            if (encPrivateKey == null)
            {
                return;
            }
            await _stateService.SetPrivateKeyEncryptedAsync(encPrivateKey);
            _privateKey = null;
        }

        public async Task<byte[]> GetUserPrivateKeyAsync()
        {
            if (_privateKey != null)
            {
                return _privateKey;
            }
            var encPrivateKey = await _stateService.GetPrivateKeyEncryptedAsync();
            if (encPrivateKey == null)
            {
                return null;
            }
            _privateKey = await DecryptToBytesAsync(new EncString(encPrivateKey), null);
            return _privateKey;
        }

        public async Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null)
        {
            if (publicKey == null)
            {
                publicKey = await GetUserPublicKeyAsync();
            }
            if (publicKey == null)
            {
                throw new Exception("No public key available.");
            }
            var keyFingerprint = await _cryptoFunctionService.HashAsync(publicKey, CryptoHashAlgorithm.Sha256);
            var userFingerprint = await _cryptoFunctionService.HkdfExpandAsync(keyFingerprint, Encoding.UTF8.GetBytes(userId), 32, HkdfAlgorithm.Sha256);
            return HashPhrase(userFingerprint);
        }

        public async Task<Tuple<string, EncString>> MakeKeyPairAsync(SymmetricCryptoKey key = null)
        {
            var keyPair = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);
            var publicB64 = Convert.ToBase64String(keyPair.Item1);
            var privateEnc = await EncryptAsync(keyPair.Item2, key);
            return new Tuple<string, EncString>(publicB64, privateEnc);
        }

        public async Task ClearKeyPairAsync(bool memoryOnly = false, string userId = null)
        {
            _publicKey = _privateKey = null;
            if (!memoryOnly)
            {
                await _stateService.SetPrivateKeyEncryptedAsync(null, userId);
            }
        }

        public async Task<PinKey> MakePinKeyAsync(string pin, string salt, KdfConfig config)
        {
            var pinKey = await MakeKeyAsync(pin, salt, config, keyBytes => new PinKey(keyBytes));
            return await StretchKeyAsync(pinKey, keyBytes => new PinKey(keyBytes));
        }

        public Task ClearPinKeysAsync(string userId = null)
        {
            return Task.WhenAll(
                _stateService.SetPinKeyEncryptedUserKeyAsync(null, userId),
                _stateService.SetPinKeyEncryptedUserKeyEphemeralAsync(null, userId),
                _stateService.SetProtectedPinAsync(null, userId),
                ClearDeprecatedPinKeysAsync(userId));
        }

        public async Task<UserKey> DecryptUserKeyWithPinAsync(string pin, string salt, KdfConfig kdfConfig, EncString pinProtectedUserKey = null)
        {
            pinProtectedUserKey ??= await _stateService.GetPinKeyEncryptedUserKeyAsync();
            pinProtectedUserKey ??= await _stateService.GetPinKeyEncryptedUserKeyEphemeralAsync();
            if (pinProtectedUserKey == null)
            {
                throw new Exception("No PIN protected user key found.");
            }
            var pinKey = await MakePinKeyAsync(pin, salt, kdfConfig);
            var userKeyBytes = await DecryptToBytesAsync(pinProtectedUserKey, pinKey);
            return new UserKey(userKeyBytes);
        }

        // Only for migration purposes
        public async Task<MasterKey> DecryptMasterKeyWithPinAsync(
            string pin,
            string salt,
            KdfConfig kdfConfig,
            EncString pinProtectedMasterKey = null)
        {
            if (pinProtectedMasterKey == null)
            {
                var pinProtectedMasterKeyString = await _stateService.GetPinProtectedAsync();
                if (pinProtectedMasterKeyString == null)
                {
                    throw new Exception("No PIN protected master key found.");
                }
                pinProtectedMasterKey = new EncString(pinProtectedMasterKeyString);
            }
            var pinKey = await MakePinKeyAsync(pin, salt, kdfConfig);
            var masterKey = await DecryptToBytesAsync(pinProtectedMasterKey, pinKey);
            return new MasterKey(masterKey);
        }

        public async Task<SymmetricCryptoKey> MakeSendKeyAsync(byte[] keyMaterial)
        {
            var sendKey = await _cryptoFunctionService.HkdfAsync(keyMaterial, "bitwarden-send", "send", 64, HkdfAlgorithm.Sha256);
            return new SymmetricCryptoKey(sendKey);
        }

        public async Task<EncString> RsaEncryptAsync(byte[] data, byte[] publicKey = null)
        {
            if (publicKey == null)
            {
                publicKey = await GetUserPublicKeyAsync();
            }
            if (publicKey == null)
            {
                throw new Exception("Public key unavailable.");
            }
            var encBytes = await _cryptoFunctionService.RsaEncryptAsync(data, publicKey, CryptoHashAlgorithm.Sha1);
            return new EncString(EncryptionType.Rsa2048_OaepSha1_B64, Convert.ToBase64String(encBytes));
        }

        public async Task<byte[]> RsaDecryptAsync(string encValue, byte[] privateKey = null)
        {
            var headerPieces = encValue.Split('.');
            EncryptionType? encType = null;
            string[] encPieces = null;

            if (headerPieces.Length == 1)
            {
                encType = EncryptionType.Rsa2048_OaepSha256_B64;
                encPieces = new string[] { headerPieces[0] };
            }
            else if (headerPieces.Length == 2 && Enum.TryParse(headerPieces[0], out EncryptionType type))
            {
                encType = type;
                encPieces = headerPieces[1].Split('|');
            }

            if (!encType.HasValue)
            {
                throw new Exception("encType unavailable.");
            }
            if (encPieces == null || encPieces.Length == 0)
            {
                throw new Exception("encPieces unavailable.");
            }

            var data = Convert.FromBase64String(encPieces[0]);

            if (privateKey is null)
            {
                privateKey = await GetUserPrivateKeyAsync();
            }

            if (privateKey == null)
            {
                throw new Exception("No private key.");
            }

            var alg = CryptoHashAlgorithm.Sha1;
            switch (encType.Value)
            {
                case EncryptionType.Rsa2048_OaepSha256_B64:
                case EncryptionType.Rsa2048_OaepSha256_HmacSha256_B64:
                    alg = CryptoHashAlgorithm.Sha256;
                    break;
                case EncryptionType.Rsa2048_OaepSha1_B64:
                case EncryptionType.Rsa2048_OaepSha1_HmacSha256_B64:
                    break;
                default:
                    throw new Exception("encType unavailable.");
            }

            return await _cryptoFunctionService.RsaDecryptAsync(data, privateKey, alg);
        }

        public async Task<int> RandomNumberAsync(int min, int max)
        {
            // Make max inclusive
            max = max + 1;

            var diff = (long)max - min;
            var upperBound = uint.MaxValue / diff * diff;
            uint ui;
            do
            {
                ui = await _cryptoFunctionService.RandomNumberAsync();
            } while (ui >= upperBound);
            return (int)(min + (ui % diff));
        }

        /// <summary>
        /// Makes random string with length <paramref name="length"/> based on the charset <see cref="RANDOM_STRING_CHARSET"/>
        /// </summary>
        public async Task<string> RandomStringAsync(int length)
        {
            var sb = new StringBuilder();

            for (var i = 0; i < length; i++)
            {
                var randomCharIndex = await RandomNumberAsync(0, RANDOM_STRING_CHARSET.Length - 1);
                sb.Append(RANDOM_STRING_CHARSET[randomCharIndex]);
            }

            return sb.ToString();
        }

        // TODO: The following operations should be moved to a new encrypt service

        public async Task<byte[]> DecryptFromBytesAsync(byte[] encBytes, SymmetricCryptoKey key)
        {
            if (encBytes == null)
            {
                throw new Exception("no encBytes.");
            }

            var encType = (EncryptionType)encBytes[0];
            byte[] ctBytes = null;
            byte[] ivBytes = null;
            byte[] macBytes = null;

            switch (encType)
            {
                case EncryptionType.AesCbc128_HmacSha256_B64:
                case EncryptionType.AesCbc256_HmacSha256_B64:
                    if (encBytes.Length < 49) // 1 + 16 + 32 + ctLength
                    {
                        return null;
                    }
                    ivBytes = new ArraySegment<byte>(encBytes, 1, 16).ToArray();
                    macBytes = new ArraySegment<byte>(encBytes, 17, 32).ToArray();
                    ctBytes = new ArraySegment<byte>(encBytes, 49, encBytes.Length - 49).ToArray();
                    break;
                case EncryptionType.AesCbc256_B64:
                    if (encBytes.Length < 17) // 1 + 16 + ctLength
                    {
                        return null;
                    }
                    ivBytes = new ArraySegment<byte>(encBytes, 1, 16).ToArray();
                    ctBytes = new ArraySegment<byte>(encBytes, 17, encBytes.Length - 17).ToArray();
                    break;
                default:
                    return null;
            }

            return await AesDecryptToBytesAsync(encType, ctBytes, ivBytes, macBytes, key);
        }

        public Task<byte[]> DecryptToBytesAsync(EncString encString, SymmetricCryptoKey key = null)
        {
            var iv = Convert.FromBase64String(encString.Iv);
            var data = Convert.FromBase64String(encString.Data);
            var mac = !string.IsNullOrWhiteSpace(encString.Mac) ? Convert.FromBase64String(encString.Mac) : null;
            return AesDecryptToBytesAsync(encString.EncryptionType, data, iv, mac, key);
        }

        public Task<string> DecryptToUtf8Async(EncString encString, SymmetricCryptoKey key = null)
        {
            return AesDecryptToUtf8Async(encString.EncryptionType, encString.Data,
                encString.Iv, encString.Mac, key);
        }

        public async Task<EncString> EncryptAsync(string plainValue, SymmetricCryptoKey key = null)
        {
            if (plainValue == null)
            {
                return null;
            }
            return await EncryptAsync(Encoding.UTF8.GetBytes(plainValue), key);
        }

        public async Task<EncString> EncryptAsync(byte[] plainValue, SymmetricCryptoKey key = null)
        {
            if (plainValue == null)
            {
                return null;
            }
            var encObj = await AesEncryptAsync(plainValue, key);
            var iv = Convert.ToBase64String(encObj.Iv);
            var data = Convert.ToBase64String(encObj.Data);
            var mac = encObj.Mac != null ? Convert.ToBase64String(encObj.Mac) : null;
            return new EncString(encObj.Key.EncType, data, iv, mac);
        }

        public async Task<EncByteArray> EncryptToBytesAsync(byte[] plainValue, SymmetricCryptoKey key = null)
        {
            var encValue = await AesEncryptAsync(plainValue, key);
            var macLen = 0;
            if (encValue.Mac != null)
            {
                macLen = encValue.Mac.Length;
            }
            var encBytes = new byte[1 + encValue.Iv.Length + macLen + encValue.Data.Length];
            Buffer.BlockCopy(new byte[] { (byte)encValue.Key.EncType }, 0, encBytes, 0, 1);
            Buffer.BlockCopy(encValue.Iv, 0, encBytes, 1, encValue.Iv.Length);
            if (encValue.Mac != null)
            {
                Buffer.BlockCopy(encValue.Mac, 0, encBytes, 1 + encValue.Iv.Length, encValue.Mac.Length);
            }
            Buffer.BlockCopy(encValue.Data, 0, encBytes, 1 + encValue.Iv.Length + macLen, encValue.Data.Length);
            return new EncByteArray(encBytes);
        }

        public async Task<MasterKey> GetOrDeriveMasterKeyAsync(string password, string userId = null)
        {
            var masterKey = await GetMasterKeyAsync(userId);
            return masterKey ?? await this.MakeMasterKeyAsync(
                password,
                await _stateService.GetEmailAsync(userId),
                await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile)));
        }

        public async Task UpdateMasterKeyAndUserKeyAsync(MasterKey masterKey)
        {
            var userKey = await DecryptUserKeyWithMasterKeyAsync(masterKey);
            await SetMasterKeyAsync(masterKey);
            var hasKey = await HasUserKeyAsync();
            if (!hasKey)
            {
                await SetUserKeyAsync(userKey);
            }
        }

        // --HELPER METHODS--

        private async Task StoreAdditionalKeysAsync(UserKey userKey, string userId = null)
        {
            // Set, refresh, or clear the pin key
            if (await _stateService.GetProtectedPinAsync(userId) != null)
            {
                await UpdatePinKeyAsync(userKey, userId);
            }
            else
            {
                await _stateService.SetPinKeyEncryptedUserKeyAsync(null, userId);
                await _stateService.SetPinKeyEncryptedUserKeyEphemeralAsync(null, userId);
            }

            // Set, refresh, or clear the auto unlock key
            if (await _stateService.GetVaultTimeoutAsync(userId) == null)
            {
                await _stateService.SetUserKeyAutoUnlockAsync(userKey, userId);
            }
            else
            {
                await _stateService.SetUserKeyAutoUnlockAsync(null, userId);
            }

            // Set, refresh, or clear the biometric unlock key
            if (await _stateService.GetBiometricUnlockAsync(userId) is true)
            {
                await _stateService.SetUserKeyBiometricUnlockAsync(userKey, userId);
            }
            else
            {
                await _stateService.SetUserKeyBiometricUnlockAsync(null, userId);
            }
        }

        private async Task UpdatePinKeyAsync(UserKey userKey, string userId = null)
        {
            var pin = await DecryptToUtf8Async(new EncString(await _stateService.GetProtectedPinAsync(userId)));
            var pinKey = await MakePinKeyAsync(
                pin,
                await _stateService.GetEmailAsync(userId),
                await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile))
            );
            var encPin = await EncryptAsync(userKey.Key, pinKey);

            if (await _stateService.GetPinKeyEncryptedUserKeyAsync(userId) != null)
            {
                await _stateService.SetPinKeyEncryptedUserKeyAsync(encPin, userId);
                return;
            }
            await _stateService.SetPinKeyEncryptedUserKeyEphemeralAsync(encPin, userId);
        }

        private async Task<EncryptedObject> AesEncryptAsync(byte[] data, SymmetricCryptoKey key)
        {
            var obj = new EncryptedObject
            {
                Key = key ?? await GetUserKeyWithLegacySupportAsync(),
                Iv = await _cryptoFunctionService.RandomBytesAsync(16)
            };
            obj.Data = await _cryptoFunctionService.AesEncryptAsync(data, obj.Iv, obj.Key.EncKey);
            if (obj.Key.MacKey != null)
            {
                var macData = new byte[obj.Iv.Length + obj.Data.Length];
                Buffer.BlockCopy(obj.Iv, 0, macData, 0, obj.Iv.Length);
                Buffer.BlockCopy(obj.Data, 0, macData, obj.Iv.Length, obj.Data.Length);
                obj.Mac = await _cryptoFunctionService.HmacAsync(macData, obj.Key.MacKey, CryptoHashAlgorithm.Sha256);
            }
            return obj;
        }

        private async Task<string> AesDecryptToUtf8Async(EncryptionType encType, string data, string iv, string mac,
            SymmetricCryptoKey key)
        {
            var keyForEnc = key ?? await GetUserKeyWithLegacySupportAsync();
            var theKey = ResolveLegacyKey(encType, keyForEnc);
            if (theKey.MacKey != null && mac == null)
            {
                // Mac required.
                return null;
            }
            if (theKey.EncType != encType)
            {
                // encType unavailable.
                return null;
            }

            // "Fast params" conversion
            var encKey = theKey.EncKey;
            var dataBytes = Convert.FromBase64String(data);
            var ivBytes = Convert.FromBase64String(iv);

            var macDataBytes = new byte[ivBytes.Length + dataBytes.Length];
            Buffer.BlockCopy(ivBytes, 0, macDataBytes, 0, ivBytes.Length);
            Buffer.BlockCopy(dataBytes, 0, macDataBytes, ivBytes.Length, dataBytes.Length);

            byte[] macKey = null;
            if (theKey.MacKey != null)
            {
                macKey = theKey.MacKey;
            }
            byte[] macBytes = null;
            if (mac != null)
            {
                macBytes = Convert.FromBase64String(mac);
            }

            // Compute mac
            if (macKey != null && macBytes != null)
            {
                var computedMac = await _cryptoFunctionService.HmacAsync(macDataBytes, macKey,
                    CryptoHashAlgorithm.Sha256);
                var macsEqual = await _cryptoFunctionService.CompareAsync(macBytes, computedMac);
                if (!macsEqual)
                {
                    // Mac failed
                    return null;
                }
            }

            var decBytes = await _cryptoFunctionService.AesDecryptAsync(dataBytes, ivBytes, encKey);
            return Encoding.UTF8.GetString(decBytes);
        }

        private async Task<byte[]> AesDecryptToBytesAsync(EncryptionType encType, byte[] data, byte[] iv, byte[] mac,
            SymmetricCryptoKey key)
        {

            var keyForEnc = key ?? await GetUserKeyWithLegacySupportAsync();
            var theKey = ResolveLegacyKey(encType, keyForEnc);
            if (theKey.MacKey != null && mac == null)
            {
                // Mac required.
                return null;
            }
            if (theKey.EncType != encType)
            {
                // encType unavailable.
                return null;
            }

            // Compute mac
            if (theKey.MacKey != null && mac != null)
            {
                var macData = new byte[iv.Length + data.Length];
                Buffer.BlockCopy(iv, 0, macData, 0, iv.Length);
                Buffer.BlockCopy(data, 0, macData, iv.Length, data.Length);

                var computedMac = await _cryptoFunctionService.HmacAsync(macData, theKey.MacKey,
                    CryptoHashAlgorithm.Sha256);
                if (computedMac == null)
                {
                    return null;
                }
                var macsMatch = await _cryptoFunctionService.CompareAsync(mac, computedMac);
                if (!macsMatch)
                {
                    // Mac failed
                    return null;
                }
            }

            return await _cryptoFunctionService.AesDecryptAsync(data, iv, theKey.EncKey);
        }


        private SymmetricCryptoKey ResolveLegacyKey(EncryptionType encKey, SymmetricCryptoKey key)
        {
            if (encKey == EncryptionType.AesCbc128_HmacSha256_B64 && key.EncType == EncryptionType.AesCbc256_B64)
            {
                // Old encrypt-then-mac scheme, make a new key
                if (_legacyEtmKey == null)
                {
                    _legacyEtmKey = new SymmetricCryptoKey(key.Key, EncryptionType.AesCbc128_HmacSha256_B64);
                }
                return _legacyEtmKey;
            }
            return key;
        }

        // TODO: This needs to be moved into SymmetricCryptoKey model to remove the keyCreator hack
        private async Task<TKey> StretchKeyAsync<TKey>(SymmetricCryptoKey key, Func<byte[], TKey> keyCreator)
        where TKey : SymmetricCryptoKey
        {
            var newKey = new byte[64];
            var enc = await _cryptoFunctionService.HkdfExpandAsync(key.Key, Encoding.UTF8.GetBytes("enc"), 32, HkdfAlgorithm.Sha256);
            Buffer.BlockCopy(enc, 0, newKey, 0, 32);
            var mac = await _cryptoFunctionService.HkdfExpandAsync(key.Key, Encoding.UTF8.GetBytes("mac"), 32, HkdfAlgorithm.Sha256);
            Buffer.BlockCopy(mac, 0, newKey, 32, 32);
            return keyCreator(newKey);
        }

        private List<string> HashPhrase(byte[] hash, int minimumEntropy = 64)
        {
            var wordLength = EEFLongWordList.Instance.List.Count;
            var entropyPerWord = Math.Log(wordLength) / Math.Log(2);
            var numWords = (int)Math.Ceiling(minimumEntropy / entropyPerWord);

            var entropyAvailable = hash.Length * 4;
            if (numWords * entropyPerWord > entropyAvailable)
            {
                throw new Exception("Output entropy of hash function is too small");
            }

            var phrase = new List<string>();
            var hashHex = string.Concat("0", BitConverter.ToString(hash).Replace("-", ""));
            var hashNumber = BigInteger.Parse(hashHex, System.Globalization.NumberStyles.HexNumber);
            while (numWords-- > 0)
            {
                var remainder = (int)(hashNumber % wordLength);
                hashNumber = hashNumber / wordLength;
                phrase.Add(EEFLongWordList.Instance.List[remainder]);
            }
            return phrase;
        }

        // TODO: This needs to be moved into SymmetricCryptoKey model to remove the keyCreator hack
        private async Task<Tuple<TKey, EncString>> BuildProtectedSymmetricKeyAsync<TKey>(SymmetricCryptoKey key,
            byte[] encKey, Func<byte[], TKey> keyCreator) where TKey : SymmetricCryptoKey
        {
            EncString encKeyEnc = null;
            if (key.Key.Length == 32)
            {
                var newKey = await StretchKeyAsync(key, keyCreator);
                encKeyEnc = await EncryptAsync(encKey, newKey);
            }
            else if (key.Key.Length == 64)
            {
                encKeyEnc = await EncryptAsync(encKey, key);
            }
            else
            {
                throw new Exception("Invalid key size.");
            }
            return new Tuple<TKey, EncString>(keyCreator(encKey), encKeyEnc);
        }

        // TODO: This needs to be moved into SymmetricCryptoKey model to remove the keyCreator hack
        private async Task<TKey> MakeKeyAsync<TKey>(string password, string salt, KdfConfig kdfConfig, Func<byte[], TKey> keyCreator)
        where TKey : SymmetricCryptoKey
        {
            byte[] key;
            if (kdfConfig.Type == null || kdfConfig.Type == KdfType.PBKDF2_SHA256)
            {
                var iterations = kdfConfig.Iterations.GetValueOrDefault(5000);
                if (iterations < 5000)
                {
                    throw new Exception("PBKDF2 iteration minimum is 5000.");
                }
                key = await _cryptoFunctionService.Pbkdf2Async(password, salt,
                    CryptoHashAlgorithm.Sha256, iterations);
            }
            else if (kdfConfig.Type == KdfType.Argon2id)
            {
                var iterations = kdfConfig.Iterations.GetValueOrDefault(Constants.Argon2Iterations);
                var memory = kdfConfig.Memory.GetValueOrDefault(Constants.Argon2MemoryInMB) * 1024;
                var parallelism = kdfConfig.Parallelism.GetValueOrDefault(Constants.Argon2Parallelism);

                if (kdfConfig.Iterations < 2)
                {
                    throw new Exception("Argon2 iterations minimum is 2");
                }

                if (kdfConfig.Memory < 16)
                {
                    throw new Exception("Argon2 memory minimum is 16 MB");
                }
                else if (kdfConfig.Memory > 1024)
                {
                    throw new Exception("Argon2 memory maximum is 1024 MB");
                }

                if (kdfConfig.Parallelism < 1)
                {
                    throw new Exception("Argon2 parallelism minimum is 1");
                }

                var saltHash = await _cryptoFunctionService.HashAsync(salt, CryptoHashAlgorithm.Sha256);
                key = await _cryptoFunctionService.Argon2Async(password, saltHash, iterations, memory, parallelism);
            }
            else
            {
                throw new Exception("Unknown kdf.");
            }
            return keyCreator(key);
        }

        private async Task<bool> ValidateUserKeyAsync(UserKey key, string userId = null)
        {
            if (key == null)
            {
                return false;
            }

            try
            {
                var encPrivateKey = await _stateService.GetPrivateKeyEncryptedAsync(userId);
                if (encPrivateKey == null)
                {
                    return false;
                }

                var privateKey = await DecryptToBytesAsync(new EncString(encPrivateKey), key);
                await _cryptoFunctionService.RsaExtractPublicKeyAsync(privateKey);
                return true;
            }
            catch
            {
                return false;
            }
        }

        private class EncryptedObject
        {
            public byte[] Iv { get; set; }
            public byte[] Data { get; set; }
            public byte[] Mac { get; set; }
            public SymmetricCryptoKey Key { get; set; }
        }

        // --MIGRATION METHODS--
        // We previously used the master key for additional keys, but now we use the user key.
        // These methods support migrating the old keys to the new ones.

        private async Task MigrateAutoAndBioKeysIfNeededAsync(string userId = null)
        {
            var oldKey = await _stateService.GetKeyEncryptedAsync(userId);
            if (oldKey == null)
            {
                return;
            }

            // Decrypt
            var masterKey = new MasterKey(Convert.FromBase64String(oldKey));
            if (await IsLegacyUserAsync(masterKey, userId))
            {
                throw new LegacyUserException();
            }
            var encryptedUserKey = await _stateService.GetEncKeyEncryptedAsync(userId);
            if (encryptedUserKey == null)
            {
                return;
            }
            var userKey = await DecryptUserKeyWithMasterKeyAsync(
                masterKey,
                new EncString(encryptedUserKey),
                userId);

            // Migrate
            if (await _stateService.GetVaultTimeoutAsync(userId) == null)
            {
                await _stateService.SetUserKeyAutoUnlockAsync(userKey, userId);
            }
            if (await _stateService.GetBiometricUnlockAsync(userId) is true)
            {
                await _stateService.SetUserKeyBiometricUnlockAsync(userKey, userId);
            }
            // Clear old enc key only if we don't need to still migrate PIN
            if (await _stateService.GetPinProtectedAsync() == null
                && await _stateService.GetPinProtectedKeyAsync() == null)
            {
                await _stateService.SetEncKeyEncryptedAsync(null, userId);
            }
            await _stateService.SetKeyEncryptedAsync(null, userId);

            // Set encrypted user key just in case the user locks without syncing
            await SetMasterKeyEncryptedUserKeyAsync(encryptedUserKey);
        }

        public async Task<UserKey> DecryptAndMigrateOldPinKeyAsync(
            bool masterPasswordOnRestart,
            string pin,
            string email,
            KdfConfig kdfConfig,
            EncString oldPinKey)
        {
            // Decrypt
            var masterKey = await DecryptMasterKeyWithPinAsync(
                pin,
                email,
                kdfConfig,
                oldPinKey
            );
            if (await IsLegacyUserAsync(masterKey))
            {
                throw new LegacyUserException();
            }
            var encUserKey = await _stateService.GetEncKeyEncryptedAsync();
            var userKey = await DecryptUserKeyWithMasterKeyAsync(
                masterKey,
                new EncString(encUserKey)
            );

            // Migrate
            var pinKey = await MakePinKeyAsync(pin, email, kdfConfig);
            var pinProtectedKey = await EncryptAsync(userKey.Key, pinKey);

            if (masterPasswordOnRestart)
            {
                await _stateService.SetPinProtectedKeyAsync(null);
                await _stateService.SetPinKeyEncryptedUserKeyEphemeralAsync(pinProtectedKey);
            }
            else
            {
                await _stateService.SetPinProtectedAsync(null);
                await _stateService.SetPinKeyEncryptedUserKeyAsync(pinProtectedKey);

                // We previously only set the protected pin if MP on Restart was enabled
                // now we set it regardless
                var encPin = await EncryptAsync(pin, userKey);
                await _stateService.SetProtectedPinAsync(encPin.EncryptedString);
            }
            // Clear old key only if not needed for bio/auto migration
            if (await _stateService.GetKeyEncryptedAsync() != null)
            {
                await _stateService.SetEncKeyEncryptedAsync(null);
            }
            return userKey;
        }

        public Task ClearDeprecatedPinKeysAsync(string userId = null)
        {
            return Task.WhenAll(
                _stateService.SetPinProtectedAsync(null, userId),
                _stateService.SetPinProtectedKeyAsync(null, userId));
        }
    }
}

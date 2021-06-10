using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Numerics;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class CryptoService : ICryptoService
    {
        private readonly IStorageService _storageService;
        private readonly IStorageService _secureStorageService;
        private readonly ICryptoFunctionService _cryptoFunctionService;

        private SymmetricCryptoKey _key;
        private SymmetricCryptoKey _encKey;
        private SymmetricCryptoKey _legacyEtmKey;
        private string _keyHash;
        private byte[] _publicKey;
        private byte[] _privateKey;
        private Dictionary<string, SymmetricCryptoKey> _orgKeys;
        private Task<SymmetricCryptoKey> _getEncKeysTask;
        private Task<Dictionary<string, SymmetricCryptoKey>> _getOrgKeysTask;

        private const string Keys_Key = "key";
        private const string Keys_EncOrgKeys = "encOrgKeys";
        private const string Keys_EncPrivateKey = "encPrivateKey";
        private const string Keys_EncKey = "encKey";
        private const string Keys_KeyHash = "keyHash";

        public CryptoService(
            IStorageService storageService,
            IStorageService secureStorageService,
            ICryptoFunctionService cryptoFunctionService)
        {
            _storageService = storageService;
            _secureStorageService = secureStorageService;
            _cryptoFunctionService = cryptoFunctionService;
        }

        public async Task SetKeyAsync(SymmetricCryptoKey key)
        {
            _key = key;
            var option = await _storageService.GetAsync<int?>(Constants.VaultTimeoutKey);
            var biometric = await _storageService.GetAsync<bool?>(Constants.BiometricUnlockKey);
            if (option.HasValue && !biometric.GetValueOrDefault())
            {
                // If we have a lock option set, we do not store the key
                return;
            }
            await _secureStorageService.SaveAsync(Keys_Key, key?.KeyB64);
        }

        public async Task SetKeyHashAsync(string keyHash)
        {
            _keyHash = keyHash;
            await _storageService.SaveAsync(Keys_KeyHash, keyHash);
        }

        public async Task SetEncKeyAsync(string encKey)
        {
            if (encKey == null)
            {
                return;
            }
            await _storageService.SaveAsync(Keys_EncKey, encKey);
            _encKey = null;
        }

        public async Task SetEncPrivateKeyAsync(string encPrivateKey)
        {
            if (encPrivateKey == null)
            {
                return;
            }
            await _storageService.SaveAsync(Keys_EncPrivateKey, encPrivateKey);
            _privateKey = null;
        }

        public async Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs)
        {
            var orgKeys = orgs.ToDictionary(org => org.Id, org => org.Key);
            _orgKeys = null;
            await _storageService.SaveAsync(Keys_EncOrgKeys, orgKeys);
        }

        public async Task<SymmetricCryptoKey> GetKeyAsync()
        {
            if (_key != null)
            {
                return _key;
            }
            var key = await _secureStorageService.GetAsync<string>(Keys_Key);
            if (key != null)
            {
                _key = new SymmetricCryptoKey(Convert.FromBase64String(key));
            }
            return _key;
        }

        public async Task<string> GetKeyHashAsync()
        {
            if (_keyHash != null)
            {
                return _keyHash;
            }
            var keyHash = await _storageService.GetAsync<string>(Keys_KeyHash);
            if (keyHash != null)
            {
                _keyHash = keyHash;
            }
            return _keyHash;
        }

        public Task<SymmetricCryptoKey> GetEncKeyAsync(SymmetricCryptoKey key = null)
        {
            if (_encKey != null)
            {
                return Task.FromResult(_encKey);
            }
            if (_getEncKeysTask != null && !_getEncKeysTask.IsCompleted && !_getEncKeysTask.IsFaulted)
            {
                return _getEncKeysTask;
            }
            async Task<SymmetricCryptoKey> doTask()
            {
                try
                {
                    var encKey = await _storageService.GetAsync<string>(Keys_EncKey);
                    if (encKey == null)
                    {
                        return null;
                    }

                    if (key == null)
                    {
                        key = await GetKeyAsync();
                    }
                    if (key == null)
                    {
                        return null;
                    }

                    byte[] decEncKey = null;
                    var encKeyCipher = new EncString(encKey);
                    if (encKeyCipher.EncryptionType == EncryptionType.AesCbc256_B64)
                    {
                        decEncKey = await DecryptToBytesAsync(encKeyCipher, key);
                    }
                    else if (encKeyCipher.EncryptionType == EncryptionType.AesCbc256_HmacSha256_B64)
                    {
                        var newKey = await StretchKeyAsync(key);
                        decEncKey = await DecryptToBytesAsync(encKeyCipher, newKey);
                    }
                    else
                    {
                        throw new Exception("Unsupported encKey type.");
                    }

                    if (decEncKey == null)
                    {
                        return null;
                    }
                    _encKey = new SymmetricCryptoKey(decEncKey);
                    return _encKey;
                }
                finally
                {
                    _getEncKeysTask = null;
                }
            }
            _getEncKeysTask = doTask();
            return _getEncKeysTask;
        }

        public async Task<byte[]> GetPublicKeyAsync()
        {
            if (_publicKey != null)
            {
                return _publicKey;
            }
            var privateKey = await GetPrivateKeyAsync();
            if (privateKey == null)
            {
                return null;
            }
            _publicKey = await _cryptoFunctionService.RsaExtractPublicKeyAsync(privateKey);
            return _publicKey;
        }

        public async Task<byte[]> GetPrivateKeyAsync()
        {
            if (_privateKey != null)
            {
                return _privateKey;
            }
            var encPrivateKey = await _storageService.GetAsync<string>(Keys_EncPrivateKey);
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
                publicKey = await GetPublicKeyAsync();
            }
            if (publicKey == null)
            {
                throw new Exception("No public key available.");
            }
            var keyFingerprint = await _cryptoFunctionService.HashAsync(publicKey, CryptoHashAlgorithm.Sha256);
            var userFingerprint = await _cryptoFunctionService.HkdfExpandAsync(keyFingerprint, Encoding.UTF8.GetBytes(userId), 32, HkdfAlgorithm.Sha256);
            return HashPhrase(userFingerprint);
        }

        public Task<Dictionary<string, SymmetricCryptoKey>> GetOrgKeysAsync()
        {
            if (_orgKeys != null && _orgKeys.Count > 0)
            {
                return Task.FromResult(_orgKeys);
            }
            if (_getOrgKeysTask != null && !_getOrgKeysTask.IsCompleted && !_getOrgKeysTask.IsFaulted)
            {
                return _getOrgKeysTask;
            }
            async Task<Dictionary<string, SymmetricCryptoKey>> doTask()
            {
                try
                {
                    var encOrgKeys = await _storageService.GetAsync<Dictionary<string, string>>(Keys_EncOrgKeys);
                    if (encOrgKeys == null)
                    {
                        return null;
                    }
                    var orgKeys = new Dictionary<string, SymmetricCryptoKey>();
                    var setKey = false;
                    foreach (var org in encOrgKeys)
                    {
                        var decValue = await RsaDecryptAsync(org.Value);
                        orgKeys.Add(org.Key, new SymmetricCryptoKey(decValue));
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

        public async Task<SymmetricCryptoKey> GetOrgKeyAsync(string orgId)
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

        public async Task<bool> CompareAndUpdateKeyHashAsync(string masterPassword, SymmetricCryptoKey key)
        {
            var storedKeyHash = await GetKeyHashAsync();
            if (masterPassword != null && storedKeyHash != null)
            {
                var localKeyHash = await HashPasswordAsync(masterPassword, key, HashPurpose.LocalAuthorization);
                if (localKeyHash != null && storedKeyHash == localKeyHash)
                {
                    return true;
                }

                var serverKeyHash = await HashPasswordAsync(masterPassword, key, HashPurpose.ServerAuthorization);
                if (serverKeyHash != null & storedKeyHash == serverKeyHash)
                {
                    await SetKeyHashAsync(localKeyHash);
                    return true;
                }
            }

            return false;
        }

        public async Task<bool> HasKeyAsync()
        {
            var key = await GetKeyAsync();
            return key != null;
        }

        public async Task<bool> HasEncKeyAsync()
        {
            var encKey = await _storageService.GetAsync<string>(Keys_EncKey);
            return encKey != null;
        }

        public async Task ClearKeyAsync()
        {
            _key = _legacyEtmKey = null;
            await _secureStorageService.RemoveAsync(Keys_Key);
        }

        public async Task ClearKeyHashAsync()
        {
            _keyHash = null;
            await _storageService.RemoveAsync(Keys_KeyHash);
        }

        public async Task ClearEncKeyAsync(bool memoryOnly = false)
        {
            _encKey = null;
            if (!memoryOnly)
            {
                await _storageService.RemoveAsync(Keys_EncKey);
            }
        }

        public async Task ClearKeyPairAsync(bool memoryOnly = false)
        {
            _publicKey = _privateKey = null;
            if (!memoryOnly)
            {
                await _storageService.RemoveAsync(Keys_EncPrivateKey);
            }
        }

        public async Task ClearOrgKeysAsync(bool memoryOnly = false)
        {
            _orgKeys = null;
            if (!memoryOnly)
            {
                await _storageService.RemoveAsync(Keys_EncOrgKeys);
            }
        }

        public async Task ClearPinProtectedKeyAsync()
        {
            await _storageService.RemoveAsync(Constants.PinProtectedKey);
        }

        public async Task ClearKeysAsync()
        {
            await Task.WhenAll(new Task[]
            {
                ClearKeyAsync(),
                ClearKeyHashAsync(),
                ClearOrgKeysAsync(),
                ClearEncKeyAsync(),
                ClearKeyPairAsync(),
                ClearPinProtectedKeyAsync()
            });
        }

        public async Task ToggleKeyAsync()
        {
            var key = await GetKeyAsync();
            var option = await _storageService.GetAsync<int?>(Constants.VaultTimeoutKey);
            var biometric = await _storageService.GetAsync<bool?>(Constants.BiometricUnlockKey);
            if (!biometric.GetValueOrDefault() && (option != null || option == 0))
            {
                await ClearKeyAsync();
                _key = key;
                return;
            }
            await SetKeyAsync(key);
        }

        public async Task<SymmetricCryptoKey> MakeKeyAsync(string password, string salt,
            KdfType? kdf, int? kdfIterations)
        {
            byte[] key = null;
            if (kdf == null || kdf == KdfType.PBKDF2_SHA256)
            {
                if (kdfIterations == null)
                {
                    kdfIterations = 5000;
                }
                if (kdfIterations < 5000)
                {
                    throw new Exception("PBKDF2 iteration minimum is 5000.");
                }
                key = await _cryptoFunctionService.Pbkdf2Async(password, salt,
                    CryptoHashAlgorithm.Sha256, kdfIterations.Value);
            }
            else
            {
                throw new Exception("Unknown kdf.");
            }
            return new SymmetricCryptoKey(key);
        }

        public async Task<SymmetricCryptoKey> MakeKeyFromPinAsync(string pin, string salt,
            KdfType kdf, int kdfIterations, EncString protectedKeyCs = null)
        {
            if (protectedKeyCs == null)
            {
                var pinProtectedKey = await _storageService.GetAsync<string>(Constants.PinProtectedKey);
                if (pinProtectedKey == null)
                {
                    throw new Exception("No PIN protected key found.");
                }
                protectedKeyCs = new EncString(pinProtectedKey);
            }
            var pinKey = await MakePinKeyAysnc(pin, salt, kdf, kdfIterations);
            var decKey = await DecryptToBytesAsync(protectedKeyCs, pinKey);
            return new SymmetricCryptoKey(decKey);
        }

        public async Task<Tuple<EncString, SymmetricCryptoKey>> MakeShareKeyAsync()
        {
            var shareKey = await _cryptoFunctionService.RandomBytesAsync(64);
            var publicKey = await GetPublicKeyAsync();
            var encShareKey = await RsaEncryptAsync(shareKey, publicKey);
            return new Tuple<EncString, SymmetricCryptoKey>(encShareKey, new SymmetricCryptoKey(shareKey));
        }

        public async Task<Tuple<string, EncString>> MakeKeyPairAsync(SymmetricCryptoKey key = null)
        {
            var keyPair = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);
            var publicB64 = Convert.ToBase64String(keyPair.Item1);
            var privateEnc = await EncryptAsync(keyPair.Item2, key);
            return new Tuple<string, EncString>(publicB64, privateEnc);
        }

        public async Task<SymmetricCryptoKey> MakePinKeyAysnc(string pin, string salt, KdfType kdf, int kdfIterations)
        {
            var pinKey = await MakeKeyAsync(pin, salt, kdf, kdfIterations);
            return await StretchKeyAsync(pinKey);
        }

        public async Task<SymmetricCryptoKey> MakeSendKeyAsync(byte[] keyMaterial)
        {
            var sendKey = await _cryptoFunctionService.HkdfAsync(keyMaterial, "bitwarden-send", "send", 64, HkdfAlgorithm.Sha256);
            return new SymmetricCryptoKey(sendKey);
        }

        public async Task<string> HashPasswordAsync(string password, SymmetricCryptoKey key, HashPurpose hashPurpose = HashPurpose.ServerAuthorization)
        {
            if (key == null)
            {
                key = await GetKeyAsync();
            }
            if (password == null || key == null)
            {
                throw new Exception("Invalid parameters.");
            }
            var iterations = hashPurpose == HashPurpose.LocalAuthorization ? 2 : 1;
            var hash = await _cryptoFunctionService.Pbkdf2Async(key.Key, password, CryptoHashAlgorithm.Sha256, iterations);
            return Convert.ToBase64String(hash);
        }

        public async Task<Tuple<SymmetricCryptoKey, EncString>> MakeEncKeyAsync(SymmetricCryptoKey key)
        {
            var theKey = await GetKeyForEncryptionAsync(key);
            var encKey = await _cryptoFunctionService.RandomBytesAsync(64);
            return await BuildEncKeyAsync(theKey, encKey);
        }

        public async Task<Tuple<SymmetricCryptoKey, EncString>> RemakeEncKeyAsync(SymmetricCryptoKey key)
        {
            var encKey = await GetEncKeyAsync();
            return await BuildEncKeyAsync(key, encKey.Key);
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

        public async Task<EncString> RsaEncryptAsync(byte[] data, byte[] publicKey = null)
        {
            if (publicKey == null)
            {
                publicKey = await GetPublicKeyAsync();
            }
            if (publicKey == null)
            {
                throw new Exception("Public key unavailable.");
            }
            var encBytes = await _cryptoFunctionService.RsaEncryptAsync(data, publicKey, CryptoHashAlgorithm.Sha1);
            return new EncString(EncryptionType.Rsa2048_OaepSha1_B64, Convert.ToBase64String(encBytes));
        }

        public async Task<byte[]> DecryptToBytesAsync(EncString encString, SymmetricCryptoKey key = null)
        {
            var iv = Convert.FromBase64String(encString.Iv);
            var data = Convert.FromBase64String(encString.Data);
            var mac = !string.IsNullOrWhiteSpace(encString.Mac) ? Convert.FromBase64String(encString.Mac) : null;
            return await AesDecryptToBytesAsync(encString.EncryptionType, data, iv, mac, key);
        }

        public async Task<string> DecryptToUtf8Async(EncString encString, SymmetricCryptoKey key = null)
        {
            return await AesDecryptToUtf8Async(encString.EncryptionType, encString.Data,
                encString.Iv, encString.Mac, key);
        }

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

        // Helpers

        private async Task<EncryptedObject> AesEncryptAsync(byte[] data, SymmetricCryptoKey key)
        {
            var obj = new EncryptedObject
            {
                Key = await GetKeyForEncryptionAsync(key),
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
            var keyForEnc = await GetKeyForEncryptionAsync(key);
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

            var keyForEnc = await GetKeyForEncryptionAsync(key);
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

        private async Task<byte[]> RsaDecryptAsync(string encValue)
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
            var privateKey = await GetPrivateKeyAsync();
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

        private async Task<SymmetricCryptoKey> GetKeyForEncryptionAsync(SymmetricCryptoKey key = null)
        {
            if (key != null)
            {
                return key;
            }
            var encKey = await GetEncKeyAsync();
            if (encKey != null)
            {
                return encKey;
            }
            return await GetKeyAsync();
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

        private async Task<SymmetricCryptoKey> StretchKeyAsync(SymmetricCryptoKey key)
        {
            var newKey = new byte[64];
            var enc = await _cryptoFunctionService.HkdfExpandAsync(key.Key, Encoding.UTF8.GetBytes("enc"), 32, HkdfAlgorithm.Sha256);
            Buffer.BlockCopy(enc, 0, newKey, 0, 32);
            var mac = await _cryptoFunctionService.HkdfExpandAsync(key.Key, Encoding.UTF8.GetBytes("mac"), 32, HkdfAlgorithm.Sha256);
            Buffer.BlockCopy(mac, 0, newKey, 32, 32);
            return new SymmetricCryptoKey(newKey);
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

        private async Task<Tuple<SymmetricCryptoKey, EncString>> BuildEncKeyAsync(SymmetricCryptoKey key,
            byte[] encKey)
        {
            EncString encKeyEnc = null;
            if (key.Key.Length == 32)
            {
                var newKey = await StretchKeyAsync(key);
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
            return new Tuple<SymmetricCryptoKey, EncString>(new SymmetricCryptoKey(encKey), encKeyEnc);
        }

        private class EncryptedObject
        {
            public byte[] Iv { get; set; }
            public byte[] Data { get; set; }
            public byte[] Mac { get; set; }
            public SymmetricCryptoKey Key { get; set; }
        }
    }
}

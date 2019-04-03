using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class CryptoService
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
            var option = await _storageService.GetAsync<int?>(Constants.LockOptionKey);
            if(option.HasValue)
            {
                // If we have a lock option set, we do not store the key
                return;
            }
            await _secureStorageService.SaveAsync(Keys_Key, key.KeyB64);
        }

        public async Task SetKeyHashAsync(string keyHash)
        {
            _keyHash = keyHash;
            await _storageService.SaveAsync(Keys_KeyHash, keyHash);
        }

        public async Task SetEncKeyAsync(string encKey)
        {
            if(encKey == null)
            {
                return;
            }
            await _storageService.SaveAsync(Keys_EncKey, encKey);
            _encKey = null;
        }

        public async Task SetEncPrivateKeyAsync(string encPrivateKey)
        {
            if(encPrivateKey == null)
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
            if(_key != null)
            {
                return _key;
            }
            var key = await _secureStorageService.GetAsync<string>(Keys_Key);
            if(key != null)
            {
                _key = new SymmetricCryptoKey(Convert.FromBase64String(key));
            }
            return key == null ? null : _key;
        }

        public async Task<string> GetKeyHashAsync()
        {
            if(_keyHash != null)
            {
                return _keyHash;
            }
            var keyHash = await _secureStorageService.GetAsync<string>(Keys_KeyHash);
            if(keyHash != null)
            {
                _keyHash = keyHash;
            }
            return keyHash == null ? null : _keyHash;
        }

        public async Task<SymmetricCryptoKey> GetEncKeyAsync()
        {
            if(_encKey != null)
            {
                return _encKey;
            }
            var encKey = await _storageService.GetAsync<string>(Keys_EncKey);
            if(encKey == null)
            {
                return null;
            }

            var key = await GetKeyAsync();
            if(key == null)
            {
                return null;
            }

            byte[] decEncKey = null;
            var encKeyCipher = new CipherString(encKey);
            if(encKeyCipher.EncryptionType == EncryptionType.AesCbc256_B64)
            {
                // TODO
            }
            else if(encKeyCipher.EncryptionType == EncryptionType.AesCbc256_HmacSha256_B64)
            {
                // TODO
            }
            else
            {
                throw new Exception("Unsupported encKey type.");
            }

            if(decEncKey == null)
            {
                return null;
            }
            _encKey = new SymmetricCryptoKey(decEncKey);
            return _encKey;
        }

        public async Task<byte[]> GetPublicKeyAsync()
        {
            if(_publicKey != null)
            {
                return _publicKey;
            }
            var privateKey = await GetPrivateKeyAsync();
            if(privateKey == null)
            {
                return null;
            }
            _publicKey = await _cryptoFunctionService.RsaExtractPublicKeyAsync(privateKey);
            return _publicKey;
        }

        public async Task<byte[]> GetPrivateKeyAsync()
        {
            if(_privateKey != null)
            {
                return _privateKey;
            }
            var encPrivateKey = await _storageService.GetAsync<string>(Keys_EncPrivateKey);
            if(encPrivateKey == null)
            {
                return null;
            }
            // TODO
            return _privateKey;
        }

        public async Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null)
        {
            if(publicKey == null)
            {
                publicKey = await GetPublicKeyAsync();
            }
            if(publicKey == null)
            {
                throw new Exception("No public key available.");
            }
            var keyFingerprint = await _cryptoFunctionService.HashAsync(publicKey, CryptoHashAlgorithm.Sha256);
            // TODO
            return null;
        }
        
    }
}

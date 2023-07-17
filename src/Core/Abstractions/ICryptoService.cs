using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface ICryptoService
    {
        Task SetUserKeyAsync(UserKey userKey);
        Task<UserKey> GetUserKeyAsync(string userId = null);
        Task<bool> HasUserKeyAsync(string userId = null);
        Task ClearUserKeyAsync(string userId = null);
        Task SetMasterKeyEncryptedUserKeyAsync(string value, string userId = null);
        Task SetMasterKeyAsync(MasterKey masterKey, string userId = null);
        Task<MasterKey> GetMasterKeyAsync(string userId = null);
        Task ClearMasterKeyAsync(string userId = null);
        Task SetPasswordHashAsync(string keyHash);
        Task<string> GetPasswordHashAsync();
        Task ClearPasswordHashAsync(string userId = null);
        Task<bool> CompareAndUpdatePasswordHashAsync(string masterPassword, SymmetricCryptoKey key);

        Task ClearEncKeyAsync(bool memoryOnly = false, string userId = null);
        Task ClearKeyAsync(string userId = null);
        Task ClearKeyPairAsync(bool memoryOnly = false, string userId = null);
        Task ClearKeysAsync(string userId = null);
        Task ClearOrgKeysAsync(bool memoryOnly = false, string userId = null);
        Task ClearPinProtectedKeyAsync(string userId = null);
        void ClearCache();
        Task<byte[]> DecryptFromBytesAsync(byte[] encBytes, SymmetricCryptoKey key);
        Task<byte[]> DecryptToBytesAsync(EncString encString, SymmetricCryptoKey key = null);
        Task<string> DecryptToUtf8Async(EncString encString, SymmetricCryptoKey key = null);
        Task<EncString> EncryptAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<EncString> EncryptAsync(string plainValue, SymmetricCryptoKey key = null);
        Task<EncByteArray> EncryptToBytesAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> GetEncKeyAsync(SymmetricCryptoKey key = null);
        Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null);
        Task<SymmetricCryptoKey> GetKeyAsync(string userId = null);
        Task<SymmetricCryptoKey> GetOrgKeyAsync(string orgId);
        Task<Dictionary<string, SymmetricCryptoKey>> GetOrgKeysAsync();
        Task<byte[]> GetPrivateKeyAsync();
        Task<byte[]> GetPublicKeyAsync();
        Task<bool> HasEncKeyAsync();
        Task<string> HashPasswordAsync(string password, SymmetricCryptoKey key, HashPurpose hashPurpose = HashPurpose.ServerAuthorization);
        Task<bool> HasKeyAsync(string userId = null);
        Task<Tuple<SymmetricCryptoKey, EncString>> MakeEncKeyAsync(SymmetricCryptoKey key);
        Task<SymmetricCryptoKey> MakeKeyAsync(string password, string salt, KdfConfig config);
        Task<SymmetricCryptoKey> MakeKeyFromPinAsync(string pin, string salt, KdfConfig config, EncString protectedKeyEs = null);
        Task<Tuple<string, EncString>> MakeKeyPairAsync(SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> MakePinKeyAysnc(string pin, string salt, KdfConfig config);
        Task<Tuple<EncString, SymmetricCryptoKey>> MakeShareKeyAsync();
        Task<SymmetricCryptoKey> MakeSendKeyAsync(byte[] keyMaterial);
        Task<int> RandomNumberAsync(int min, int max);
        Task<string> RandomStringAsync(int length);
        Task<Tuple<SymmetricCryptoKey, EncString>> RemakeEncKeyAsync(SymmetricCryptoKey key);
        Task<EncString> RsaEncryptAsync(byte[] data, byte[] publicKey = null);
        Task<byte[]> RsaDecryptAsync(string encValue, byte[] privateKey = null);
        Task SetEncKeyAsync(string encKey);
        Task SetEncPrivateKeyAsync(string encPrivateKey);
        Task SetKeyAsync(SymmetricCryptoKey key);
        Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs);
        Task ToggleKeyAsync();
    }
}

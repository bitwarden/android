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
        Task ToggleKeysAsync();
        Task SetUserKeyAsync(UserKey userKey, string userId = null);
        Task<UserKey> GetUserKeyAsync(string userId = null);
        Task<bool> HasUserKeyAsync(string userId = null);
        Task<bool> HasEncryptedUserKeyAsync(string userId = null);
        Task<UserKey> MakeUserKeyAsync();
        Task ClearUserKeyAsync(string userId = null);
        Task SetMasterKeyEncryptedUserKeyAsync(string value, string userId = null);
        Task SetMasterKeyAsync(MasterKey masterKey, string userId = null);
        Task<MasterKey> GetMasterKeyAsync(string userId = null);
        Task<MasterKey> MakeMasterKeyAsync(string password, string email, KdfConfig kdfConfig);
        Task ClearMasterKeyAsync(string userId = null);
        Task<Tuple<UserKey, EncString>> EncryptUserKeyWithMasterKeyAsync(MasterKey masterKey, UserKey userKey = null);
        Task<UserKey> DecryptUserKeyWithMasterKeyAsync(MasterKey masterKey, EncString encUserKey = null, string userId = null);
        Task<string> HashPasswordAsync(string password, SymmetricCryptoKey key, HashPurpose hashPurpose = HashPurpose.ServerAuthorization);
        Task SetPasswordHashAsync(string keyHash);
        Task<string> GetPasswordHashAsync();
        Task ClearPasswordHashAsync(string userId = null);
        Task<bool> CompareAndUpdatePasswordHashAsync(string masterPassword, MasterKey key);
        Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs);
        Task<OrgKey> GetOrgKeyAsync(string orgId);
        Task<Dictionary<string, OrgKey>> GetOrgKeysAsync();
        Task ClearOrgKeysAsync(bool memoryOnly = false, string userId = null);
        Task<byte[]> GetPublicKeyAsync();
        Task SetPrivateKeyAsync(string encPrivateKey);
        Task<byte[]> GetPrivateKeyAsync();
        Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null);
        Task<Tuple<string, EncString>> MakeKeyPairAsync(SymmetricCryptoKey key = null);
        Task ClearKeyPairAsync(bool memoryOnly = false, string userId = null);
        Task<PinKey> MakePinKeyAsync(string pin, string salt, KdfConfig config);
        Task<UserKey> DecryptUserKeyWithPinAsync(string pin, string salt, KdfConfig kdfConfig, EncString pinProtectedUserKey = null);
        Task<MasterKey> DecryptMasterKeyWithPinAsync(string pin, string salt, KdfConfig kdfConfig, EncString pinProtectedMasterKey = null);
        Task<SymmetricCryptoKey> MakeSendKeyAsync(byte[] keyMaterial);
        // TODO(Jake): This isn't used, delete?
        Task ClearKeysAsync(string userId = null);
        Task<EncString> RsaEncryptAsync(byte[] data, byte[] publicKey = null);
        Task<byte[]> RsaDecryptAsync(string encValue, byte[] privateKey = null);
        Task<int> RandomNumberAsync(int min, int max);
        Task<string> RandomStringAsync(int length);
        Task<byte[]> DecryptFromBytesAsync(byte[] encBytes, SymmetricCryptoKey key);
        Task<byte[]> DecryptToBytesAsync(EncString encString, SymmetricCryptoKey key = null);
        Task<string> DecryptToUtf8Async(EncString encString, SymmetricCryptoKey key = null);
        Task<EncString> EncryptAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<EncString> EncryptAsync(string plainValue, SymmetricCryptoKey key = null);
        Task<EncByteArray> EncryptToBytesAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<UserKey> DecryptAndMigrateOldPinKeyAsync(bool masterPasswordOnRestart, string pin, string email, KdfConfig kdfConfig, EncString oldPinKey);









        Task ClearEncKeyAsync(bool memoryOnly = false, string userId = null);
        Task ClearKeyAsync(string userId = null);
        Task ClearPinProtectedKeyAsync(string userId = null);
        void ClearCache();
        Task<SymmetricCryptoKey> GetEncKeyAsync(SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> GetKeyAsync(string userId = null);
        Task<bool> HasEncKeyAsync();
        Task<bool> HasKeyAsync(string userId = null);
        Task<Tuple<SymmetricCryptoKey, EncString>> MakeEncKeyAsync(SymmetricCryptoKey key);
        // TODO(Jake): This isn't used, delete
        Task<Tuple<EncString, SymmetricCryptoKey>> MakeShareKeyAsync();
        Task SetEncKeyAsync(string encKey);
        Task SetKeyAsync(SymmetricCryptoKey key);
    }
}

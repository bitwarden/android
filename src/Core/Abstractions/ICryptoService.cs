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
        void ClearCache();
        Task RefreshKeysAsync();
        Task SetUserKeyAsync(UserKey userKey, string userId = null);
        Task<UserKey> GetUserKeyAsync(string userId = null);
        Task<bool> IsLegacyUserAsync(MasterKey masterKey = null, string userId = null);
        Task<UserKey> GetUserKeyWithLegacySupportAsync(string userId = null);
        Task<bool> HasUserKeyAsync(string userId = null);
        Task<bool> HasEncryptedUserKeyAsync(string userId = null);
        Task<UserKey> MakeUserKeyAsync();
        Task ClearUserKeyAsync(string userId = null);
        Task SetMasterKeyEncryptedUserKeyAsync(string value, string userId = null);
        Task<UserKey> GetAutoUnlockKeyAsync(string userId = null);
        Task<bool> HasAutoUnlockKeyAsync(string userId = null);
        Task<UserKey> GetBiometricUnlockKeyAsync(string userId = null);
        Task SetMasterKeyAsync(MasterKey masterKey, string userId = null);
        Task<MasterKey> GetMasterKeyAsync(string userId = null);
        Task<MasterKey> MakeMasterKeyAsync(string password, string email, KdfConfig kdfConfig);
        Task ClearMasterKeyAsync(string userId = null);
        Task<Tuple<UserKey, EncString>> EncryptUserKeyWithMasterKeyAsync(MasterKey masterKey, UserKey userKey = null);
        Task<UserKey> DecryptUserKeyWithMasterKeyAsync(MasterKey masterKey, EncString encUserKey = null, string userId = null);
        Task<Tuple<SymmetricCryptoKey, EncString>> MakeDataEncKeyAsync(SymmetricCryptoKey key);
        Task<string> HashMasterKeyAsync(string password, MasterKey key, HashPurpose hashPurpose = HashPurpose.ServerAuthorization);
        Task SetMasterKeyHashAsync(string keyHash);
        Task<string> GetMasterKeyHashAsync();
        Task ClearMasterKeyHashAsync(string userId = null);
        Task<bool> CompareAndUpdateKeyHashAsync(string masterPassword, MasterKey key);
        Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs);
        Task<OrgKey> GetOrgKeyAsync(string orgId);
        Task<Dictionary<string, OrgKey>> GetOrgKeysAsync();
        Task ClearOrgKeysAsync(bool memoryOnly = false, string userId = null);
        Task<byte[]> GetUserPublicKeyAsync();
        Task SetUserPrivateKeyAsync(string encPrivateKey);
        Task<byte[]> GetUserPrivateKeyAsync();
        Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null);
        Task<Tuple<string, EncString>> MakeKeyPairAsync(SymmetricCryptoKey key = null);
        Task ClearKeyPairAsync(bool memoryOnly = false, string userId = null);
        Task<PinKey> MakePinKeyAsync(string pin, string salt, KdfConfig config);
        Task ClearPinKeysAsync(string userId = null);
        Task<UserKey> DecryptUserKeyWithPinAsync(string pin, string salt, KdfConfig kdfConfig, EncString pinProtectedUserKey = null);
        Task<MasterKey> DecryptMasterKeyWithPinAsync(string pin, string salt, KdfConfig kdfConfig, EncString pinProtectedMasterKey = null);
        Task<SymmetricCryptoKey> MakeSendKeyAsync(byte[] keyMaterial);
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
        Task<MasterKey> GetOrDeriveMasterKeyAsync(string password, string userId = null);
        Task UpdateMasterKeyAndUserKeyAsync(MasterKey masterKey);
    }
}

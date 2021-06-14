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
        Task ClearEncKeyAsync(bool memoryOnly = false);
        Task ClearKeyAsync();
        Task ClearKeyHashAsync();
        Task ClearKeyPairAsync(bool memoryOnly = false);
        Task ClearKeysAsync();
        Task ClearOrgKeysAsync(bool memoryOnly = false);
        Task ClearPinProtectedKeyAsync();
        Task<byte[]> DecryptFromBytesAsync(byte[] encBytes, SymmetricCryptoKey key);
        Task<byte[]> DecryptToBytesAsync(EncString encString, SymmetricCryptoKey key = null);
        Task<string> DecryptToUtf8Async(EncString encString, SymmetricCryptoKey key = null);
        Task<EncString> EncryptAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<EncString> EncryptAsync(string plainValue, SymmetricCryptoKey key = null);
        Task<EncByteArray> EncryptToBytesAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> GetEncKeyAsync(SymmetricCryptoKey key = null);
        Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null);
        Task<SymmetricCryptoKey> GetKeyAsync();
        Task<string> GetKeyHashAsync();
        Task<SymmetricCryptoKey> GetOrgKeyAsync(string orgId);
        Task<Dictionary<string, SymmetricCryptoKey>> GetOrgKeysAsync();
        Task<byte[]> GetPrivateKeyAsync();
        Task<byte[]> GetPublicKeyAsync();
        Task<bool> CompareAndUpdateKeyHashAsync(string masterPassword, SymmetricCryptoKey key);
        Task<bool> HasEncKeyAsync();
        Task<string> HashPasswordAsync(string password, SymmetricCryptoKey key, HashPurpose hashPurpose = HashPurpose.ServerAuthorization);
        Task<bool> HasKeyAsync();
        Task<Tuple<SymmetricCryptoKey, EncString>> MakeEncKeyAsync(SymmetricCryptoKey key);
        Task<SymmetricCryptoKey> MakeKeyAsync(string password, string salt, KdfType? kdf, int? kdfIterations);
        Task<SymmetricCryptoKey> MakeKeyFromPinAsync(string pin, string salt, KdfType kdf, int kdfIterations,
            EncString protectedKeyEs = null);
        Task<Tuple<string, EncString>> MakeKeyPairAsync(SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> MakePinKeyAysnc(string pin, string salt, KdfType kdf, int kdfIterations);
        Task<Tuple<EncString, SymmetricCryptoKey>> MakeShareKeyAsync();
        Task<SymmetricCryptoKey> MakeSendKeyAsync(byte[] keyMaterial);
        Task<int> RandomNumberAsync(int min, int max);
        Task<Tuple<SymmetricCryptoKey, EncString>> RemakeEncKeyAsync(SymmetricCryptoKey key);
        Task<EncString> RsaEncryptAsync(byte[] data, byte[] publicKey = null);
        Task SetEncKeyAsync(string encKey);
        Task SetEncPrivateKeyAsync(string encPrivateKey);
        Task SetKeyAsync(SymmetricCryptoKey key);
        Task SetKeyHashAsync(string keyHash);
        Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs);
        Task ToggleKeyAsync();
    }
}

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
        Task<byte[]> DecryptToBytesAsync(CipherString cipherString, SymmetricCryptoKey key = null);
        Task<string> DecryptToUtf8Async(CipherString cipherString, SymmetricCryptoKey key = null);
        Task<CipherString> EncryptAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<CipherString> EncryptAsync(string plainValue, SymmetricCryptoKey key = null);
        Task<byte[]> EncryptToBytesAsync(byte[] plainValue, SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> GetEncKeyAsync(SymmetricCryptoKey key = null);
        Task<List<string>> GetFingerprintAsync(string userId, byte[] publicKey = null);
        Task<SymmetricCryptoKey> GetKeyAsync();
        Task<string> GetKeyHashAsync();
        Task<SymmetricCryptoKey> GetOrgKeyAsync(string orgId);
        Task<Dictionary<string, SymmetricCryptoKey>> GetOrgKeysAsync();
        Task<byte[]> GetPrivateKeyAsync();
        Task<byte[]> GetPublicKeyAsync();
        Task<bool> HasEncKeyAsync();
        Task<string> HashPasswordAsync(string password, SymmetricCryptoKey key);
        Task<bool> HasKeyAsync();
        Task<Tuple<SymmetricCryptoKey, CipherString>> MakeEncKeyAsync(SymmetricCryptoKey key);
        Task<SymmetricCryptoKey> MakeKeyAsync(string password, string salt, KdfType? kdf, int? kdfIterations);
        Task<SymmetricCryptoKey> MakeKeyFromPinAsync(string pin, string salt, KdfType kdf, int kdfIterations,
            CipherString protectedKeyCs = null);
        Task<Tuple<string, CipherString>> MakeKeyPairAsync(SymmetricCryptoKey key = null);
        Task<SymmetricCryptoKey> MakePinKeyAysnc(string pin, string salt, KdfType kdf, int kdfIterations);
        Task<Tuple<CipherString, SymmetricCryptoKey>> MakeShareKeyAsync();
        Task<int> RandomNumberAsync(int min, int max);
        Task<Tuple<SymmetricCryptoKey, CipherString>> RemakeEncKeyAsync(SymmetricCryptoKey key);
        Task<CipherString> RsaEncryptAsync(byte[] data, byte[] publicKey = null);
        Task SetEncKeyAsync(string encKey);
        Task SetEncPrivateKeyAsync(string encPrivateKey);
        Task SetKeyAsync(SymmetricCryptoKey key);
        Task SetKeyHashAsync(string keyHash);
        Task SetOrgKeysAsync(IEnumerable<ProfileOrganizationResponse> orgs);
        Task ToggleKeyAsync();
        Task<byte[]> HkdfAsync(byte[] ikm, string salt, string info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfAsync(byte[] ikm, byte[] salt, string info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfAsync(byte[] ikm, string salt, byte[] info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfAsync(byte[] ikm, byte[] salt, byte[] info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfExpandAsync(byte[] prk, string info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfExpandAsync(byte[] prk, byte[] info, int outputByteSize, HkdfAlgorithm algorithm);
    }
}

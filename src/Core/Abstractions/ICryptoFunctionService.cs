using System;
using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface ICryptoFunctionService
    {
        Task<byte[]> Pbkdf2Async(string password, string salt, CryptoHashAlgorithm algorithm, int iterations);
        Task<byte[]> Pbkdf2Async(byte[] password, string salt, CryptoHashAlgorithm algorithm, int iterations);
        Task<byte[]> Pbkdf2Async(string password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations);
        Task<byte[]> Pbkdf2Async(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations);
        Task<byte[]> Argon2Async(string password, string salt, int iterations, int memory, int parallelism);
        Task<byte[]> Argon2Async(byte[] password, string salt, int iterations, int memory, int parallelism);
        Task<byte[]> Argon2Async(string password, byte[] salt, int iterations, int memory, int parallelism);
        Task<byte[]> Argon2Async(byte[] password, byte[] salt, int iterations, int memory, int parallelism);
        Task<byte[]> HkdfAsync(byte[] ikm, string salt, string info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfAsync(byte[] ikm, byte[] salt, string info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfAsync(byte[] ikm, string salt, byte[] info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfAsync(byte[] ikm, byte[] salt, byte[] info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfExpandAsync(byte[] prk, string info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HkdfExpandAsync(byte[] prk, byte[] info, int outputByteSize, HkdfAlgorithm algorithm);
        Task<byte[]> HashAsync(string value, CryptoHashAlgorithm algorithm);
        Task<byte[]> HashAsync(byte[] value, CryptoHashAlgorithm algorithm);
        Task<byte[]> HmacAsync(byte[] value, byte[] key, CryptoHashAlgorithm algorithm);
        Task<bool> CompareAsync(byte[] a, byte[] b);
        Task<byte[]> AesEncryptAsync(byte[] data, byte[] iv, byte[] key);
        Task<byte[]> AesDecryptAsync(byte[] data, byte[] iv, byte[] key);
        Task<byte[]> RsaEncryptAsync(byte[] data, byte[] publicKey, CryptoHashAlgorithm algorithm);
        Task<byte[]> RsaDecryptAsync(byte[] data, byte[] privateKey, CryptoHashAlgorithm algorithm);
        Task<byte[]> RsaExtractPublicKeyAsync(byte[] privateKey);
        Task<Tuple<byte[], byte[]>> RsaGenerateKeyPairAsync(int length);
        Task<byte[]> RandomBytesAsync(int length);
        byte[] RandomBytes(int length);
        Task<uint> RandomNumberAsync();
        uint RandomNumber();
    }
}

using System;
using System.Linq;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;

namespace Bit.Core.Services
{
    public class CryptoFunctionService : ICryptoFunctionService
    {
        private readonly ICryptoPrimitiveService _cryptoPrimitiveService;

        public CryptoFunctionService(ICryptoPrimitiveService cryptoPrimitiveService)
        {
            _cryptoPrimitiveService = cryptoPrimitiveService;
        }

        public Task<byte[]> Pbkdf2Async(string password, string salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            password = NormalizePassword(password);
            return Pbkdf2Async(Encoding.UTF8.GetBytes(password), Encoding.UTF8.GetBytes(salt), algorithm, iterations);
        }

        public Task<byte[]> Pbkdf2Async(byte[] password, string salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            return Pbkdf2Async(password, Encoding.UTF8.GetBytes(salt), algorithm, iterations);
        }

        public Task<byte[]> Pbkdf2Async(string password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            password = NormalizePassword(password);
            return Pbkdf2Async(Encoding.UTF8.GetBytes(password), salt, algorithm, iterations);
        }

        public Task<byte[]> Pbkdf2Async(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            if (algorithm != CryptoHashAlgorithm.Sha256 && algorithm != CryptoHashAlgorithm.Sha512)
            {
                throw new ArgumentException("Unsupported PBKDF2 algorithm.");
            }
            return Task.FromResult(_cryptoPrimitiveService.Pbkdf2(password, salt, algorithm, iterations));
        }

        // TODO: Replace with System.Security.Cryptography.HKDF when we are on .NET 6+.
        public async Task<byte[]> HkdfAsync(byte[] ikm, string salt, string info, int outputByteSize, HkdfAlgorithm algorithm) =>
            await HkdfAsync(ikm, Encoding.UTF8.GetBytes(salt), Encoding.UTF8.GetBytes(info), outputByteSize, algorithm);

        public async Task<byte[]> HkdfAsync(byte[] ikm, byte[] salt, string info, int outputByteSize, HkdfAlgorithm algorithm) =>
            await HkdfAsync(ikm, salt, Encoding.UTF8.GetBytes(info), outputByteSize, algorithm);

        public async Task<byte[]> HkdfAsync(byte[] ikm, string salt, byte[] info, int outputByteSize, HkdfAlgorithm algorithm) =>
            await HkdfAsync(ikm, Encoding.UTF8.GetBytes(salt), info, outputByteSize, algorithm);

        public async Task<byte[]> HkdfAsync(byte[] ikm, byte[] salt, byte[] info, int outputByteSize, HkdfAlgorithm algorithm)
        {
            var prk = await HmacAsync(ikm, salt, HkdfAlgorithmToCryptoHashAlgorithm(algorithm));
            return await HkdfExpandAsync(prk, info, outputByteSize, algorithm);
        }

        public async Task<byte[]> HkdfExpandAsync(byte[] prk, string info, int outputByteSize, HkdfAlgorithm algorithm) =>
            await HkdfExpandAsync(prk, Encoding.UTF8.GetBytes(info), outputByteSize, algorithm);

        // ref: https://tools.ietf.org/html/rfc5869
        public async Task<byte[]> HkdfExpandAsync(byte[] prk, byte[] info, int outputByteSize, HkdfAlgorithm algorithm)
        {
            var hashLen = algorithm == HkdfAlgorithm.Sha256 ? 32 : 64;

            var maxOutputByteSize = 255 * hashLen;
            if (outputByteSize > maxOutputByteSize)
            {
                throw new ArgumentException($"{nameof(outputByteSize)} is too large. Max is {maxOutputByteSize}, received {outputByteSize}");
            }
            if (prk.Length < hashLen)
            {
                throw new ArgumentException($"{nameof(prk)} length is too small. Must be at least {hashLen} for {algorithm}");
            }

            var cryptoHashAlgorithm = HkdfAlgorithmToCryptoHashAlgorithm(algorithm);
            var previousT = new byte[0];
            var runningOkmLength = 0;
            var n = (int)Math.Ceiling((double)outputByteSize / hashLen);
            var okm = new byte[n * hashLen];
            for (var i = 0; i < n; i++)
            {
                var t = new byte[previousT.Length + info.Length + 1];
                previousT.CopyTo(t, 0);
                info.CopyTo(t, previousT.Length);
                t[t.Length - 1] = (byte)(i + 1);
                previousT = await HmacAsync(t, prk, cryptoHashAlgorithm);
                previousT.CopyTo(okm, runningOkmLength);
                runningOkmLength = previousT.Length;
                if (runningOkmLength >= outputByteSize)
                {
                    break;
                }
            }
            return okm.Take(outputByteSize).ToArray();
        }

        public Task<byte[]> HashAsync(string value, CryptoHashAlgorithm algorithm)
        {
            return HashAsync(Encoding.UTF8.GetBytes(value), algorithm);
        }

        public Task<byte[]> HashAsync(byte[] value, CryptoHashAlgorithm algorithm)
        {
            var hash = IncrementalHash.CreateHash(ToHashAlgorithmName(algorithm));
            hash.AppendData(value);
            return Task.FromResult(hash.GetHashAndReset());
        }

        public Task<byte[]> HmacAsync(byte[] value, byte[] key, CryptoHashAlgorithm algorithm)
        {
            var hash = IncrementalHash.CreateHMAC(ToHashAlgorithmName(algorithm), key);
            hash.AppendData(value);
            return Task.FromResult(hash.GetHashAndReset());
        }

        public Task<bool> CompareAsync(byte[] a, byte[] b) =>
            Task.FromResult(CryptographicOperations.FixedTimeEquals(a, b));

        private static Aes CreateAes(byte[] iv, byte[] key)
        {
            var aes = Aes.Create();
            aes.Key = key;
            aes.IV = iv;
            aes.Mode = CipherMode.CBC;
            aes.Padding = PaddingMode.PKCS7;
            return aes;
        }

        public Task<byte[]> AesEncryptAsync(byte[] data, byte[] iv, byte[] key)
        {
            var aes = CreateAes(iv, key);
            var transform = aes.CreateEncryptor(key, iv);
            return Task.FromResult(transform.TransformFinalBlock(data, 0, data.Length));
        }

        public Task<byte[]> AesDecryptAsync(byte[] data, byte[] iv, byte[] key)
        {
            var aes = CreateAes(iv, key);
            var transform = aes.CreateDecryptor(key, iv);
            return Task.FromResult(transform.TransformFinalBlock(data, 0, data.Length));
        }

        public Task<byte[]> RsaEncryptAsync(byte[] data, byte[] publicKey, CryptoHashAlgorithm algorithm)
        {
            var rsa = RSA.Create();
            rsa.ImportSubjectPublicKeyInfo(publicKey, out _);
            return Task.FromResult(rsa.Encrypt(data, ToRSAEncryptionPadding(algorithm)));
        }

        public Task<byte[]> RsaDecryptAsync(byte[] data, byte[] privateKey, CryptoHashAlgorithm algorithm)
        {
            var rsa = RSA.Create();
            rsa.ImportPkcs8PrivateKey(privateKey, out _);
            return Task.FromResult(rsa.Decrypt(data, ToRSAEncryptionPadding(algorithm)));
        }

        public Task<byte[]> RsaExtractPublicKeyAsync(byte[] privateKey)
        {
            // Have to specify some algorithm
            var rsa = RSA.Create();
            rsa.ImportPkcs8PrivateKey(privateKey, out _);
            return Task.FromResult(rsa.ExportSubjectPublicKeyInfo());
        }

        public Task<Tuple<byte[], byte[]>> RsaGenerateKeyPairAsync(int length)
        {
            if (length != 1024 && length != 2048 && length != 4096)
            {
                throw new ArgumentException("Invalid key pair length.");
            }

            // Have to specify some algorithm
            var rsa = RSA.Create(length);
            var publicKey = rsa.ExportSubjectPublicKeyInfo();
            var privateKey = rsa.ExportPkcs8PrivateKey();
            return Task.FromResult(new Tuple<byte[], byte[]>(publicKey, privateKey));
        }

        public Task<byte[]> RandomBytesAsync(int length)
        {
            return Task.FromResult(RandomBytes(length));
        }

        public byte[] RandomBytes(int length)
        {
            byte[] result = new byte[length];
            RandomNumberGenerator.Fill(result);
            return result;
        }

        public Task<uint> RandomNumberAsync()
        {
            return Task.FromResult(RandomNumber());
        }

        public uint RandomNumber()
        {
            Span<byte> bytes = stackalloc byte[sizeof(uint)];
            RandomNumberGenerator.Fill(bytes);
            return MemoryMarshal.Read<uint>(bytes);
        }

        private HashAlgorithmName ToHashAlgorithmName(CryptoHashAlgorithm algorithm)
        {
            switch (algorithm)
            {
                case CryptoHashAlgorithm.Sha1:
                    return HashAlgorithmName.SHA1;
                case CryptoHashAlgorithm.Sha256:
                    return HashAlgorithmName.SHA256;
                case CryptoHashAlgorithm.Sha512:
                    return HashAlgorithmName.SHA512;
                case CryptoHashAlgorithm.Md5:
                    return HashAlgorithmName.MD5;
                default:
                    throw new ArgumentException("Unsupported hash algorithm.");
            }
        }

        private RSAEncryptionPadding ToRSAEncryptionPadding(CryptoHashAlgorithm algorithm)
        {
            switch (algorithm)
            {
                case CryptoHashAlgorithm.Sha1:
                    return RSAEncryptionPadding.OaepSHA1;
                case CryptoHashAlgorithm.Sha256:
                    return RSAEncryptionPadding.OaepSHA256;
                case CryptoHashAlgorithm.Sha512:
                    return RSAEncryptionPadding.OaepSHA512;
                default:
                    throw new ArgumentException("Unsupported asymmetric algorithm.");
            }
        }

        // Some users like to copy/paste passwords from external files. Sometimes this can lead to two different
        // values on mobiles apps vs the web. For example, on Android an EditText will accept a new line character
        // (\n), whereas whenever you paste a new line character on the web in a HTML input box it is converted
        // to a space ( ). Normalize those values so that they are the same on all platforms.
        private string NormalizePassword(string password)
        {
            return password
                .Replace("\r\n", " ") // Windows-style new line => space
                .Replace("\n", " ") // New line => space
                .Replace(" ", " "); // No-break space (00A0) => space
        }

        private CryptoHashAlgorithm HkdfAlgorithmToCryptoHashAlgorithm(HkdfAlgorithm hkdfAlgorithm)
        {
            switch (hkdfAlgorithm)
            {
                case HkdfAlgorithm.Sha256:
                    return CryptoHashAlgorithm.Sha256;
                case HkdfAlgorithm.Sha512:
                    return CryptoHashAlgorithm.Sha512;
                default:
                    throw new ArgumentException($"Invalid hkdf algorithm type, {hkdfAlgorithm}");
            }
        }
    }
}

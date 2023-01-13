using System;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using PCLCrypto;
using static PCLCrypto.WinRTCrypto;

namespace Bit.Core.Services
{
    public class PclCryptoFunctionService : ICryptoFunctionService
    {
        private readonly ICryptoPrimitiveService _cryptoPrimitiveService;

        public PclCryptoFunctionService(ICryptoPrimitiveService cryptoPrimitiveService)
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

        public Task<byte[]> Argon2Async(string password, string salt, int iterations, int memory, int parallelism)
        {
            password = NormalizePassword(password);
            return Argon2Async(Encoding.UTF8.GetBytes(password), Encoding.UTF8.GetBytes(salt), iterations, memory, parallelism);
        }

        public Task<byte[]> Argon2Async(byte[] password, string salt, int iterations, int memory, int parallelism)
        {
            return Argon2Async(password, Encoding.UTF8.GetBytes(salt), iterations, memory, parallelism);
        }

        public Task<byte[]> Argon2Async(string password, byte[] salt, int iterations, int memory, int parallelism)
        {
            password = NormalizePassword(password);
            return Argon2Async(Encoding.UTF8.GetBytes(password), salt, iterations, memory, parallelism);
        }
        
        public Task<byte[]> Argon2Async(byte[] password, byte[] salt, int iterations, int memory, int parallelism)
        {
            return Task.FromResult(_cryptoPrimitiveService.Argon2id(password, salt, iterations, memory, parallelism));
        }

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
            var provider = HashAlgorithmProvider.OpenAlgorithm(ToHashAlgorithm(algorithm));
            return Task.FromResult(provider.HashData(value));
        }

        public Task<byte[]> HmacAsync(byte[] value, byte[] key, CryptoHashAlgorithm algorithm)
        {
            var provider = MacAlgorithmProvider.OpenAlgorithm(ToMacAlgorithm(algorithm));
            var hasher = provider.CreateHash(key);
            hasher.Append(value);
            return Task.FromResult(hasher.GetValueAndReset());
        }

        public async Task<bool> CompareAsync(byte[] a, byte[] b)
        {
            var provider = MacAlgorithmProvider.OpenAlgorithm(MacAlgorithm.HmacSha256);
            var hasher = provider.CreateHash(await RandomBytesAsync(32));

            hasher.Append(a);
            var mac1 = hasher.GetValueAndReset();
            hasher.Append(b);
            var mac2 = hasher.GetValueAndReset();
            if (mac1.Length != mac2.Length)
            {
                return false;
            }

            for (int i = 0; i < mac2.Length; i++)
            {
                if (mac1[i] != mac2[i])
                {
                    return false;
                }
            }

            return true;
        }

        public Task<byte[]> AesEncryptAsync(byte[] data, byte[] iv, byte[] key)
        {
            var provider = SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            var cryptoKey = provider.CreateSymmetricKey(key);
            return Task.FromResult(CryptographicEngine.Encrypt(cryptoKey, data, iv));
        }

        public Task<byte[]> AesDecryptAsync(byte[] data, byte[] iv, byte[] key)
        {
            var provider = SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            var cryptoKey = provider.CreateSymmetricKey(key);
            return Task.FromResult(CryptographicEngine.Decrypt(cryptoKey, data, iv));
        }

        public Task<byte[]> RsaEncryptAsync(byte[] data, byte[] publicKey, CryptoHashAlgorithm algorithm)
        {
            var provider = AsymmetricKeyAlgorithmProvider.OpenAlgorithm(ToAsymmetricAlgorithm(algorithm));
            var cryptoKey = provider.ImportPublicKey(publicKey,
                CryptographicPublicKeyBlobType.X509SubjectPublicKeyInfo);
            return Task.FromResult(CryptographicEngine.Encrypt(cryptoKey, data));
        }

        public Task<byte[]> RsaDecryptAsync(byte[] data, byte[] privateKey, CryptoHashAlgorithm algorithm)
        {
            var provider = AsymmetricKeyAlgorithmProvider.OpenAlgorithm(ToAsymmetricAlgorithm(algorithm));
            var cryptoKey = provider.ImportKeyPair(privateKey, CryptographicPrivateKeyBlobType.Pkcs8RawPrivateKeyInfo);
            return Task.FromResult(CryptographicEngine.Decrypt(cryptoKey, data));
        }

        public Task<byte[]> RsaExtractPublicKeyAsync(byte[] privateKey)
        {
            // Have to specify some algorithm
            var provider = AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha1);
            var cryptoKey = provider.ImportKeyPair(privateKey, CryptographicPrivateKeyBlobType.Pkcs8RawPrivateKeyInfo);
            return Task.FromResult(cryptoKey.ExportPublicKey(CryptographicPublicKeyBlobType.X509SubjectPublicKeyInfo));
        }

        public Task<Tuple<byte[], byte[]>> RsaGenerateKeyPairAsync(int length)
        {
            if (length != 1024 && length != 2048 && length != 4096)
            {
                throw new ArgumentException("Invalid key pair length.");
            }

            // Have to specify some algorithm
            var provider = AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha1);
            var cryptoKey = provider.CreateKeyPair(length);
            var publicKey = cryptoKey.ExportPublicKey(CryptographicPublicKeyBlobType.X509SubjectPublicKeyInfo);
            var privateKey = cryptoKey.Export(CryptographicPrivateKeyBlobType.Pkcs8RawPrivateKeyInfo);
            return Task.FromResult(new Tuple<byte[], byte[]>(publicKey, privateKey));
        }

        public Task<byte[]> RandomBytesAsync(int length)
        {
            return Task.FromResult(CryptographicBuffer.GenerateRandom(length));
        }

        public byte[] RandomBytes(int length)
        {
            return CryptographicBuffer.GenerateRandom(length);
        }

        public Task<uint> RandomNumberAsync()
        {
            return Task.FromResult(CryptographicBuffer.GenerateRandomNumber());
        }

        public uint RandomNumber()
        {
            return CryptographicBuffer.GenerateRandomNumber();
        }

        private HashAlgorithm ToHashAlgorithm(CryptoHashAlgorithm algorithm)
        {
            switch (algorithm)
            {
                case CryptoHashAlgorithm.Sha1:
                    return HashAlgorithm.Sha1;
                case CryptoHashAlgorithm.Sha256:
                    return HashAlgorithm.Sha256;
                case CryptoHashAlgorithm.Sha512:
                    return HashAlgorithm.Sha512;
                case CryptoHashAlgorithm.Md5:
                    return HashAlgorithm.Md5;
                default:
                    throw new ArgumentException("Unsupported hash algorithm.");
            }
        }

        private MacAlgorithm ToMacAlgorithm(CryptoHashAlgorithm algorithm)
        {
            switch (algorithm)
            {
                case CryptoHashAlgorithm.Sha1:
                    return MacAlgorithm.HmacSha1;
                case CryptoHashAlgorithm.Sha256:
                    return MacAlgorithm.HmacSha256;
                case CryptoHashAlgorithm.Sha512:
                    return MacAlgorithm.HmacSha512;
                default:
                    throw new ArgumentException("Unsupported mac algorithm.");
            }
        }

        private AsymmetricAlgorithm ToAsymmetricAlgorithm(CryptoHashAlgorithm algorithm)
        {
            switch (algorithm)
            {
                case CryptoHashAlgorithm.Sha1:
                    return AsymmetricAlgorithm.RsaOaepSha1;
                // RsaOaepSha256 is not supported on iOS
                // ref: https://github.com/AArnott/PCLCrypto/issues/124
                // case CryptoHashAlgorithm.SHA256:
                //    return AsymmetricAlgorithm.RsaOaepSha256;
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

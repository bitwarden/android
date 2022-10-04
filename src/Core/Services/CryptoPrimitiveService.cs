using Bit.Core.Abstractions;
using Bit.Core.Enums;
using System;
using System.Security.Cryptography;

namespace Bit.Core.Services
{
    public class CryptoPrimitiveService : ICryptoPrimitiveService
    {
        public byte[] Pbkdf2(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            int keySize;
            HashAlgorithmName digest;
            if (algorithm == CryptoHashAlgorithm.Sha256)
            {
                keySize = 256;
                digest = HashAlgorithmName.SHA256;
            }
            else if (algorithm == CryptoHashAlgorithm.Sha512)
            {
                keySize = 512;
                digest = HashAlgorithmName.SHA512;
            }
            else
            {
                throw new ArgumentException("Unsupported PBKDF2 algorithm.");
            }

            var generator = new Rfc2898DeriveBytes(password, salt, iterations, digest);
            return generator.GetBytes(keySize);
        }
    }
}

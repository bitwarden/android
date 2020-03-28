using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using System;

namespace Bit.Droid.Services
{
    public class CryptoPrimitiveService : ICryptoPrimitiveService
    {
        public byte[] Pbkdf2(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            int keySize = 256;
            IDigest digest = null;
            if (algorithm == CryptoHashAlgorithm.Sha256)
            {
                keySize = 256;
                digest = new Sha256Digest();
            }
            else if (algorithm == CryptoHashAlgorithm.Sha512)
            {
                keySize = 512;
                digest = new Sha512Digest();
            }
            else
            {
                throw new ArgumentException("Unsupported PBKDF2 algorithm.");
            }

            var generator = new Pkcs5S2ParametersGenerator(digest);
            generator.Init(password, salt, iterations);
            return ((KeyParameter)generator.GenerateDerivedMacParameters(keySize)).GetKey();
        }
    }
}

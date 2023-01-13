using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using Isopoh.Cryptography.Argon2;
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
        public byte[] Argon2id(byte[] password, byte[] salt, int iterations, int memory, int parallelism)
        {
            var config = new Argon2Config();
            config.Password = password;
            config.Salt = salt;
            config.TimeCost = iterations;
            config.MemoryCost = memory;
            config.Lanes = parallelism;
            config.HashLength = 32;
            config.Threads = parallelism;
            config.Type = Argon2Type.HybridAddressing;
            var argon2 = new Argon2(config);
            return argon2.Hash().Buffer;
        }
    }
}

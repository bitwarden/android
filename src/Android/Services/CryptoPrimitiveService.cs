using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using System;
using System.Runtime.InteropServices;
using Java.Lang;

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
            JavaSystem.LoadLibrary("argon2");
            var hash = new byte[32];
            argon2id_hash_raw(iterations, memory, parallelism, password, password.Length, password, salt.Length, hash, 32);
            return hash.ToArray();
        }

        [DllImport("argon2", EntryPoint = "argon2id_hash_raw")]
        internal static extern int argon2id_hash_raw(int timeCost, int memoryCost, int parallelism, byte[] pwd, int pwdlen, byte[] salt, int saltlen, ref byte hash, int hashlen);
    }
}

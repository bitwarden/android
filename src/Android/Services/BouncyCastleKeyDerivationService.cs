using System;
using Bit.App.Abstractions;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Parameters;

namespace Bit.Android.Services
{
    public class BouncyCastleKeyDerivationService : IKeyDerivationService
    {
        private const int KeyLength = 256; // 32 bytes

        public byte[] DeriveKey(byte[] password, byte[] salt, uint rounds)
        {
            var generator = new Pkcs5S2ParametersGenerator(new Sha256Digest());
            generator.Init(password, salt, Convert.ToInt32(rounds));
            return ((KeyParameter)generator.GenerateDerivedMacParameters(KeyLength)).GetKey();
        }
    }
}

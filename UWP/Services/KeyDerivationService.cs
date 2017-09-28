using Bit.App.Abstractions;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Windows.Security.Cryptography;
using Windows.Security.Cryptography.Core;
using Windows.Storage.Streams;

namespace Bit.UWP.Services
{
    public class KeyDerivationService : IKeyDerivationService
    {
        private const int KeyLength = 256; // 32 bytes

        //todo review this
        public byte[] DeriveKey(byte[] password, byte[] salt, uint rounds)
        {
            var generator = new Pkcs5S2ParametersGenerator(new Sha256Digest());
            generator.Init(password, salt, Convert.ToInt32(rounds));
            return ((KeyParameter)generator.GenerateDerivedMacParameters(KeyLength)).GetKey();
        }
    }
}

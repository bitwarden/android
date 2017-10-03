using Bit.App.Abstractions;
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
        private const int KeyLength = 32; // 32 bytes

        public byte[] DeriveKey(byte[] password, byte[] salt, uint rounds)
        {
            IBuffer buffSalt = salt.AsBuffer();
            IBuffer buffPassword = password.AsBuffer();
            KeyDerivationAlgorithmProvider provider = KeyDerivationAlgorithmProvider.OpenAlgorithm(KeyDerivationAlgorithmNames.Pbkdf2Sha256);
            KeyDerivationParameters pbkdf2Params = KeyDerivationParameters.BuildForPbkdf2(buffSalt, rounds);
            CryptographicKey keyOriginal = provider.CreateKey(buffPassword);

            IBuffer keyDerived = CryptographicEngine.DeriveKeyMaterial(
                keyOriginal,
                pbkdf2Params,
                KeyLength
               );

            return keyDerived.ToArray();
        }
    }
}

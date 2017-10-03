using Bit.App.Abstractions;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Security.Cryptography.Core;

namespace Bit.UWP.Services
{
    public class KeyDerivationService : IKeyDerivationService
    {
        private const int KeyLength = 32; // 32 bytes

        public byte[] DeriveKey(byte[] password, byte[] salt, uint rounds)
        {
            var buffSalt = salt.AsBuffer();
            var buffPassword = password.AsBuffer();
            var provider = KeyDerivationAlgorithmProvider.OpenAlgorithm(KeyDerivationAlgorithmNames.Pbkdf2Sha256);
            var pbkdf2Params = KeyDerivationParameters.BuildForPbkdf2(buffSalt, rounds);
            var keyOriginal = provider.CreateKey(buffPassword);

            var keyDerived = CryptographicEngine.DeriveKeyMaterial(keyOriginal, pbkdf2Params, KeyLength);
            return keyDerived.ToArray();
        }
    }
}

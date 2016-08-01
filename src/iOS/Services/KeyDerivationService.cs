using Bit.App.Abstractions;
using Foundation;
using System;
using System.Runtime.InteropServices;
using System.Text;

namespace Bit.iOS.Services
{
    public class KeyDerivationService : IKeyDerivationService
    {
        private const uint PBKDFAlgorithm = 2; // PBKDF2
        private const uint PseudoRandomAlgorithm = 3; // SHA256
        private const uint Rounds = 5000;

        public byte[] DeriveKey(string password, string salt)
        {
            var passwordData = NSData.FromArray(Encoding.UTF8.GetBytes(password));
            var saltData = NSData.FromArray(Encoding.UTF8.GetBytes(salt));

            var keyData = new NSMutableData();
            keyData.Length = 32;
            var result = CCKeyCerivationPBKDF(PBKDFAlgorithm, passwordData.Bytes, passwordData.Length, saltData.Bytes, 
                saltData.Length, PseudoRandomAlgorithm, Rounds, keyData.MutableBytes, keyData.Length);

            byte[] keyBytes = new byte[keyData.Length];
            Marshal.Copy(keyData.Bytes, keyBytes, 0, Convert.ToInt32(keyData.Length));
            return keyBytes;
        }

        // ref: http://opensource.apple.com//source/CommonCrypto/CommonCrypto-55010/CommonCrypto/CommonKeyDerivation.h
        [DllImport(ObjCRuntime.Constants.libSystemLibrary, EntryPoint = "CCKeyDerivationPBKDF")]
        public extern static int CCKeyCerivationPBKDF(uint algorithm, IntPtr password, nuint passwordLen,
            IntPtr salt, nuint saltLen, uint prf, nuint rounds, IntPtr derivedKey, nuint derivedKeyLength);
    }
}

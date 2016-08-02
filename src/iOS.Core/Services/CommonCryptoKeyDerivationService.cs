using Bit.App.Abstractions;
using Foundation;
using System;
using System.Runtime.InteropServices;

namespace Bit.iOS.Core.Services
{
    public class CommonCryptoKeyDerivationService : IKeyDerivationService
    {
        private const uint PBKDFAlgorithm = 2; // PBKDF2
        private const uint PseudoRandomAlgorithm = 3; // SHA256
        private const uint KeyLength = 32; // 256 bit

        public byte[] DeriveKey(byte[] password, byte[] salt, uint rounds)
        {
            var passwordData = NSData.FromArray(password);
            var saltData = NSData.FromArray(salt);

            var keyData = new NSMutableData();
            keyData.Length = KeyLength;
            var result = CCKeyCerivationPBKDF(PBKDFAlgorithm, passwordData.Bytes, passwordData.Length, saltData.Bytes,
                saltData.Length, PseudoRandomAlgorithm, rounds, keyData.MutableBytes, keyData.Length);

            byte[] keyBytes = new byte[keyData.Length];
            Marshal.Copy(keyData.Bytes, keyBytes, 0, Convert.ToInt32(keyData.Length));
            return keyBytes;
        }

        // ref: http://opensource.apple.com//source/CommonCrypto/CommonCrypto-55010/CommonCrypto/CommonKeyDerivation.h
        [DllImport(ObjCRuntime.Constants.libSystemLibrary, EntryPoint = "CCKeyDerivationPBKDF")]
        private extern static int CCKeyCerivationPBKDF(uint algorithm, IntPtr password, nuint passwordLen,
            IntPtr salt, nuint saltLen, uint prf, nuint rounds, IntPtr derivedKey, nuint derivedKeyLength);
    }
}

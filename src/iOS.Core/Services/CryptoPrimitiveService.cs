using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Foundation;
using System;
using System.Runtime.InteropServices;

namespace Bit.iOS.Core.Services
{
    public class CryptoPrimitiveService : ICryptoPrimitiveService
    {
        private const uint PBKDFAlgorithm = 2; // kCCPBKDF2

        public byte[] Pbkdf2(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations)
        {
            uint keySize = 32;
            uint pseudoRandomAlgorithm = 3; // kCCPRFHmacAlgSHA256
            if (algorithm == CryptoHashAlgorithm.Sha512)
            {
                keySize = 64;
                pseudoRandomAlgorithm = 5; // kCCPRFHmacAlgSHA512
            }
            else if (algorithm != CryptoHashAlgorithm.Sha256)
            {
                throw new ArgumentException("Unsupported PBKDF2 algorithm.");
            }

            var keyData = new NSMutableData();
            keyData.Length = keySize;

            var passwordData = NSData.FromArray(password);
            var saltData = NSData.FromArray(salt);

            var result = CCKeyCerivationPBKDF(PBKDFAlgorithm, passwordData.Bytes, passwordData.Length, saltData.Bytes,
                saltData.Length, pseudoRandomAlgorithm, Convert.ToUInt32(iterations), keyData.MutableBytes,
                keyData.Length);

            byte[] keyBytes = new byte[keyData.Length];
            Marshal.Copy(keyData.Bytes, keyBytes, 0, Convert.ToInt32(keyData.Length));
            return keyBytes;
        }

        // ref: http://opensource.apple.com/source/CommonCrypto/CommonCrypto-55010/CommonCrypto/CommonKeyDerivation.h
        [DllImport(ObjCRuntime.Constants.libSystemLibrary, EntryPoint = "CCKeyDerivationPBKDF")]
        private extern static int CCKeyCerivationPBKDF(uint algorithm, IntPtr password, nuint passwordLen,
            IntPtr salt, nuint saltLen, uint prf, nuint rounds, IntPtr derivedKey, nuint derivedKeyLength);
    }
}

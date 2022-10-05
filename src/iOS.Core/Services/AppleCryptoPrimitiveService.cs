using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Foundation;
using System;
using System.Runtime.InteropServices;

namespace Bit.iOS.Core.Services
{
    // TODO: Once we move to MAUI and .NET 8+ (that has a native built-in PBKDF2 implementation),
    // remove this class and fold ICryptoPrimitiveService to ICryptoFunctionService.
    public class AppleCryptoPrimitiveService : ICryptoPrimitiveService
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

            var result = CCKeyDerivationPBKDF(PBKDFAlgorithm, passwordData.Bytes, passwordData.Length, saltData.Bytes,
                saltData.Length, pseudoRandomAlgorithm, Convert.ToUInt32(iterations), keyData.MutableBytes,
                keyData.Length);

            byte[] keyBytes = new byte[keyData.Length];
            Marshal.Copy(keyData.Bytes, keyBytes, 0, Convert.ToInt32(keyData.Length));
            return keyBytes;
        }

        // ref: https://github.com/apple-oss-distributions/CommonCrypto/blob/06c4e37bc508d502031035d8606ade275d6c0d82/include/CommonKeyDerivation.h#L96-L100
        [DllImport(ObjCRuntime.Constants.libSystemLibrary, EntryPoint = "CCKeyDerivationPBKDF")]
        private extern static int CCKeyDerivationPBKDF(uint algorithm, IntPtr password, nuint passwordLen,
            IntPtr salt, nuint saltLen, uint prf, nuint rounds, IntPtr derivedKey, nuint derivedKeyLength);
    }
}

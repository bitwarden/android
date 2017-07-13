using Bit.App.Enums;
using Bit.App.Models;
using PCLCrypto;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Utilities
{
    public static class Crypto
    {
        public static CipherString AesCbcEncrypt(byte[] plainBytes, SymmetricCryptoKey key)
        {
            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(plainBytes == null)
            {
                throw new ArgumentNullException(nameof(plainBytes));
            }

            var provider = WinRTCrypto.SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            var cryptoKey = provider.CreateSymmetricKey(key.EncKey);
            var iv = RandomBytes(provider.BlockLength);
            var encryptedBytes = WinRTCrypto.CryptographicEngine.Encrypt(cryptoKey, plainBytes, iv);
            var mac = key.MacKey != null ? ComputeMacBase64(encryptedBytes, iv, key.MacKey) : null;

            return new CipherString(key.EncryptionType, Convert.ToBase64String(iv),
                Convert.ToBase64String(encryptedBytes), mac);
        }

        public static byte[] AesCbcDecrypt(CipherString encyptedValue, SymmetricCryptoKey key)
        {
            if(encyptedValue == null)
            {
                throw new ArgumentNullException(nameof(encyptedValue));
            }

            return AesCbcDecrypt(encyptedValue.EncryptionType, encyptedValue.CipherTextBytes,
                encyptedValue.InitializationVectorBytes, encyptedValue.MacBytes, key);
        }

        public static byte[] AesCbcDecrypt(EncryptionType type, byte[] ct, byte[] iv, byte[] mac, SymmetricCryptoKey key)
        {
            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(ct == null)
            {
                throw new ArgumentNullException(nameof(ct));
            }

            if(iv == null)
            {
                throw new ArgumentNullException(nameof(iv));
            }

            if(key.EncryptionType != type)
            {
                throw new InvalidOperationException(nameof(type));
            }

            if(key.MacKey != null && mac != null)
            {
                var computedMacBytes = ComputeMac(ct, iv, key.MacKey);
                if(!MacsEqual(key.MacKey, computedMacBytes, mac))
                {
                    throw new InvalidOperationException("MAC failed.");
                }
            }

            var provider = WinRTCrypto.SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            var cryptoKey = provider.CreateSymmetricKey(key.EncKey);
            var decryptedBytes = WinRTCrypto.CryptographicEngine.Decrypt(cryptoKey, ct, iv);
            return decryptedBytes;
        }

        public static byte[] RandomBytes(int length)
        {
            return WinRTCrypto.CryptographicBuffer.GenerateRandom(length);
        }

        public static string ComputeMacBase64(byte[] ctBytes, byte[] ivBytes, byte[] macKey)
        {
            var mac = ComputeMac(ctBytes, ivBytes, macKey);
            return Convert.ToBase64String(mac);
        }

        public static byte[] ComputeMac(byte[] ctBytes, byte[] ivBytes, byte[] macKey)
        {
            if(ctBytes == null)
            {
                throw new ArgumentNullException(nameof(ctBytes));
            }

            if(ivBytes == null)
            {
                throw new ArgumentNullException(nameof(ivBytes));
            }

            return ComputeMac(ivBytes.Concat(ctBytes), macKey);
        }

        public static byte[] ComputeMac(IEnumerable<byte> dataBytes, byte[] macKey)
        {
            if(macKey == null)
            {
                throw new ArgumentNullException(nameof(macKey));
            }

            if(dataBytes == null)
            {
                throw new ArgumentNullException(nameof(dataBytes));
            }

            var algorithm = WinRTCrypto.MacAlgorithmProvider.OpenAlgorithm(MacAlgorithm.HmacSha256);
            var hasher = algorithm.CreateHash(macKey);
            hasher.Append(dataBytes.ToArray());
            var mac = hasher.GetValueAndReset();
            return mac;
        }

        // Safely compare two MACs in a way that protects against timing attacks (Double HMAC Verification).
        // ref: https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2011/february/double-hmac-verification/
        public static bool MacsEqual(byte[] macKey, byte[] mac1, byte[] mac2)
        {
            var algorithm = WinRTCrypto.MacAlgorithmProvider.OpenAlgorithm(MacAlgorithm.HmacSha256);
            var hasher = algorithm.CreateHash(macKey);

            hasher.Append(mac1);
            mac1 = hasher.GetValueAndReset();

            hasher.Append(mac2);
            mac2 = hasher.GetValueAndReset();

            if(mac1.Length != mac2.Length)
            {
                return false;
            }

            for(int i = 0; i < mac2.Length; i++)
            {
                if(mac1[i] != mac2[i])
                {
                    return false;
                }
            }

            return true;
        }
    }
}

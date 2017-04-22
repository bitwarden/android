using Bit.App.Enums;
using System;
using System.Linq;

namespace Bit.App.Models
{
    public class SymmetricCryptoKey
    {
        public SymmetricCryptoKey(byte[] rawBytes, EncryptionType? encType = null)
        {
            if(rawBytes == null || rawBytes.Length == 0)
            {
                throw new Exception("Must provide keyBytes.");
            }

            if(encType == null)
            {
                if(rawBytes.Length == 32)
                {
                    encType = EncryptionType.AesCbc256_B64;
                }
                else if(rawBytes.Length == 64)
                {
                    encType = EncryptionType.AesCbc256_HmacSha256_B64;
                }
                else
                {
                    throw new Exception("Unable to determine encType.");
                }
            }

            EncryptionType = encType.Value;
            Key = rawBytes;

            if(EncryptionType == EncryptionType.AesCbc256_B64 && Key.Length == 32)
            {
                EncKey = Key;
                MacKey = null;
            }
            else if(EncryptionType == EncryptionType.AesCbc128_HmacSha256_B64 && Key.Length == 32)
            {
                EncKey = Key.Take(16).ToArray();
                MacKey = Key.Skip(16).Take(16).ToArray();
            }
            else if(EncryptionType == EncryptionType.AesCbc256_HmacSha256_B64 && Key.Length == 64)
            {
                EncKey = Key.Take(32).ToArray();
                MacKey = Key.Skip(32).Take(32).ToArray();
            }
            else
            {
                throw new Exception("Unsupported encType/key length.");
            }
        }

        public byte[] Key { get; set; }
        public string B64Key => Convert.ToBase64String(Key);
        public byte[] EncKey { get; set; }
        public byte[] MacKey { get; set; }
        public EncryptionType EncryptionType { get; set; }
    }
}

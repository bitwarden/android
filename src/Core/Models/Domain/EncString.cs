using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class EncString
    {
        private string _decryptedValue;

        public EncString(EncryptionType encryptionType, string data, string iv = null, string mac = null)
        {
            if (string.IsNullOrWhiteSpace(data))
            {
                throw new ArgumentNullException(nameof(data));
            }

            if (!string.IsNullOrWhiteSpace(iv))
            {
                EncryptedString = string.Format("{0}.{1}|{2}", (byte)encryptionType, iv, data);
            }
            else
            {
                EncryptedString = string.Format("{0}.{1}", (byte)encryptionType, data);
            }

            if (!string.IsNullOrWhiteSpace(mac))
            {
                EncryptedString = string.Format("{0}|{1}", EncryptedString, mac);
            }

            EncryptionType = encryptionType;
            Data = data;
            Iv = iv;
            Mac = mac;
        }

        public EncString(string encryptedString)
        {
            if (string.IsNullOrWhiteSpace(encryptedString))
            {
                throw new ArgumentException(nameof(encryptedString));
            }

            EncryptedString = encryptedString;
            var headerPieces = EncryptedString.Split('.');
            string[] encPieces;

            if (headerPieces.Length == 2 && Enum.TryParse(headerPieces[0], out EncryptionType encType))
            {
                EncryptionType = encType;
                encPieces = headerPieces[1].Split('|');
            }
            else
            {
                encPieces = EncryptedString.Split('|');
                EncryptionType = encPieces.Length == 3 ? EncryptionType.AesCbc128_HmacSha256_B64 :
                    EncryptionType.AesCbc256_B64;
            }

            switch (EncryptionType)
            {
                case EncryptionType.AesCbc128_HmacSha256_B64:
                case EncryptionType.AesCbc256_HmacSha256_B64:
                    if (encPieces.Length != 3)
                    {
                        return;
                    }
                    Iv = encPieces[0];
                    Data = encPieces[1];
                    Mac = encPieces[2];
                    break;
                case EncryptionType.AesCbc256_B64:
                    if (encPieces.Length != 2)
                    {
                        return;
                    }
                    Iv = encPieces[0];
                    Data = encPieces[1];
                    break;
                case EncryptionType.Rsa2048_OaepSha256_B64:
                case EncryptionType.Rsa2048_OaepSha1_B64:
                    if (encPieces.Length != 1)
                    {
                        return;
                    }
                    Data = encPieces[0];
                    break;
                default:
                    return;
            }
        }

        public EncryptionType EncryptionType { get; private set; }
        public string EncryptedString { get; private set; }
        public string Iv { get; private set; }
        public string Data { get; private set; }
        public string Mac { get; private set; }

        public async Task<string> DecryptAsync(string orgId = null, SymmetricCryptoKey key = null)
        {
            if (_decryptedValue != null)
            {
                return _decryptedValue;
            }

            var cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            try
            {
                if (key == null)
                {
                    key = await cryptoService.GetOrgKeyAsync(orgId);
                }
                _decryptedValue = await cryptoService.DecryptToUtf8Async(this, key);
            }
            catch
            {
                _decryptedValue = "[error: cannot decrypt]";
            }
            return _decryptedValue;
        }
    }
}

using System;
using Bit.App.Abstractions;
using XLabs.Ioc;

namespace Bit.App.Models
{
    public class CipherString
    {
        private string _decryptedValue;

        public CipherString(string encryptedString)
        {
            if(string.IsNullOrWhiteSpace(encryptedString) || !encryptedString.Contains("|"))
            {
                throw new ArgumentException(nameof(encryptedString));
            }

            EncryptedString = encryptedString;
        }

        public CipherString(string initializationVector, string cipherText, string mac = null)
        {
            if(string.IsNullOrWhiteSpace(initializationVector))
            {
                throw new ArgumentNullException(nameof(initializationVector));
            }

            if(string.IsNullOrWhiteSpace(cipherText))
            {
                throw new ArgumentNullException(nameof(cipherText));
            }

            EncryptedString = string.Format("{0}|{1}", initializationVector, cipherText);

            if(!string.IsNullOrWhiteSpace(mac))
            {
                EncryptedString = string.Format("{0}|{1}", EncryptedString, mac);
            }
        }

        public string EncryptedString { get; private set; }
        public string InitializationVector => EncryptedString?.Split('|')[0] ?? null;
        public string CipherText => EncryptedString?.Split('|')[1] ?? null;
        public string Mac
        {
            get
            {
                var pieces = EncryptedString?.Split('|') ?? new string[0];
                if(pieces.Length > 2)
                {
                    return pieces[2];
                }

                return null;
            }
        }
        public byte[] InitializationVectorBytes => Convert.FromBase64String(InitializationVector);
        public byte[] CipherTextBytes => Convert.FromBase64String(CipherText);
        public byte[] MacBytes => Mac == null ? null : Convert.FromBase64String(Mac);

        public string Decrypt()
        {
            if(_decryptedValue == null)
            {
                var cryptoService = Resolver.Resolve<ICryptoService>();
                _decryptedValue = cryptoService.Decrypt(this);
            }

            return _decryptedValue;
        }
    }
}

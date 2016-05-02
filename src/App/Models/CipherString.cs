using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using XLabs.Ioc;

namespace Bit.App.Models
{
    public class CipherString
    {
        public CipherString(string encryptedString)
        {
            if(string.IsNullOrWhiteSpace(encryptedString) || !encryptedString.Contains("|"))
            {
                throw new ArgumentException(nameof(encryptedString));
            }

            EncryptedString = encryptedString;
        }

        public CipherString(string initializationVector, string cipherText)
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
        }

        public string EncryptedString { get; private set; }
        public string InitializationVector { get { return EncryptedString?.Split('|')[0]; } }
        public string CipherText { get { return EncryptedString?.Split('|')[1]; } }
        public byte[] InitializationVectorBytes { get { return Convert.FromBase64String(InitializationVector); } }
        public byte[] CipherTextBytes { get { return Convert.FromBase64String(CipherText); } }

        public string Decrypt()
        {
            var cryptoService = Resolver.Resolve<ICryptoService>();
            return cryptoService.Decrypt(this);
        }
    }
}

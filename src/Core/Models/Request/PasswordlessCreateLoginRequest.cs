using System;
namespace Bit.Core.Models.Request
{
    public class PasswordlessCreateLoginRequest
    {
        public PasswordlessCreateLoginRequest(string email, string publicKey, string deviceIdentifier, string accessCode, AuthRequestType? type, string fingerprintPhrase)
        {
            Email = email ?? throw new ArgumentNullException(nameof(email));
            PublicKey = publicKey ?? throw new ArgumentNullException(nameof(publicKey));
            DeviceIdentifier = deviceIdentifier ?? throw new ArgumentNullException(nameof(deviceIdentifier));
            AccessCode = accessCode ?? throw new ArgumentNullException(nameof(accessCode));
            Type = type;
            FingerprintPhrase = fingerprintPhrase ?? throw new ArgumentNullException(nameof(fingerprintPhrase));
        }

        public string Email { get; set; }

        public string PublicKey { get; set; }

        public string DeviceIdentifier { get; set; }

        public string AccessCode { get; set; }

        public AuthRequestType? Type { get; set; }

        public string FingerprintPhrase { get; set; }
    }

    public enum AuthRequestType : byte
    {
        AuthenticateAndUnlock = 0,
        Unlock = 1
    }
}

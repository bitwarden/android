using Bit.App.Models;

namespace Bit.App.Abstractions
{
    public interface ICryptoService
    {
        CryptoKey Key { get; set; }
        CryptoKey PreviousKey { get; }
        bool KeyChanged { get; }

        string Decrypt(CipherString encyptedValue, CryptoKey key = null);
        CipherString Encrypt(string plaintextValue, CryptoKey key = null);
        CryptoKey MakeKeyFromPassword(string password, string salt);
        string MakeKeyFromPasswordBase64(string password, string salt);
        byte[] HashPassword(CryptoKey key, string password);
        string HashPasswordBase64(CryptoKey key, string password);
    }
}
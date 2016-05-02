using Bit.App.Models;

namespace Bit.App.Abstractions
{
    public interface ICryptoService
    {
        string Base64Key { get; }
        byte[] Key { get; set; }

        string Decrypt(CipherString encyptedValue);
        CipherString Encrypt(string plaintextValue);
        byte[] MakeKeyFromPassword(string password, string salt);
        string MakeKeyFromPasswordBase64(string password, string salt);
        byte[] HashPassword(byte[] key, string password);
        string HashPasswordBase64(byte[] key, string password);
    }
}
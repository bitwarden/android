using Bit.App.Models;
using System;
using System.Collections.Generic;

namespace Bit.App.Abstractions
{
    public interface ICryptoService
    {
        CryptoKey Key { get; set; }
        CryptoKey PreviousKey { get; }
        bool KeyChanged { get; }
        byte[] PrivateKey { get; }
        IDictionary<string, CryptoKey> OrgKeys { get; set; }

        void SetPrivateKey(CipherString privateKeyEnc, CryptoKey key);
        CryptoKey GetOrgKey(string orgId);
        void ClearOrgKey(string orgId);
        void ClearKeys();
        CryptoKey AddOrgKey(string orgId, CipherString encOrgKey, byte[] privateKey);
        string Decrypt(CipherString encyptedValue, CryptoKey key = null);
        byte[] DecryptToBytes(CipherString encyptedValue, CryptoKey key = null);
        byte[] RsaDecryptToBytes(CipherString encyptedValue, byte[] privateKey);
        CipherString Encrypt(string plaintextValue, CryptoKey key = null);
        CryptoKey MakeKeyFromPassword(string password, string salt);
        string MakeKeyFromPasswordBase64(string password, string salt);
        byte[] HashPassword(CryptoKey key, string password);
        string HashPasswordBase64(CryptoKey key, string password);
    }
}
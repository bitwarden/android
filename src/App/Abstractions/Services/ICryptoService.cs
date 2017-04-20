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
        IDictionary<Guid, CryptoKey> OrgKeys { get; set; }

        void SetPrivateKey(CipherString privateKeyEnc, CryptoKey key);
        CryptoKey GetOrgKey(Guid orgId);
        void ClearOrgKey(Guid orgId);
        void ClearKeys();
        CryptoKey AddOrgKey(Guid orgId, CipherString encOrgKey, byte[] privateKey);
        string Decrypt(CipherString encyptedValue, CryptoKey key = null);
        CipherString Encrypt(string plaintextValue, CryptoKey key = null);
        CryptoKey MakeKeyFromPassword(string password, string salt);
        string MakeKeyFromPasswordBase64(string password, string salt);
        byte[] HashPassword(CryptoKey key, string password);
        string HashPasswordBase64(CryptoKey key, string password);
    }
}
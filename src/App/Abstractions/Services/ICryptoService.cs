using Bit.App.Models;
using System;
using System.Collections.Generic;

namespace Bit.App.Abstractions
{
    public interface ICryptoService
    {
        SymmetricCryptoKey Key { get; set; }
        SymmetricCryptoKey PreviousKey { get; }
        bool KeyChanged { get; }
        byte[] PrivateKey { get; }
        IDictionary<string, SymmetricCryptoKey> OrgKeys { get; set; }

        void SetPrivateKey(CipherString privateKeyEnc, SymmetricCryptoKey key);
        SymmetricCryptoKey GetOrgKey(string orgId);
        void ClearOrgKey(string orgId);
        void ClearKeys();
        SymmetricCryptoKey AddOrgKey(string orgId, CipherString encOrgKey, byte[] privateKey);
        string Decrypt(CipherString encyptedValue, SymmetricCryptoKey key = null);
        byte[] DecryptToBytes(CipherString encyptedValue, SymmetricCryptoKey key = null);
        byte[] RsaDecryptToBytes(CipherString encyptedValue, byte[] privateKey);
        CipherString Encrypt(string plaintextValue, SymmetricCryptoKey key = null);
        SymmetricCryptoKey MakeKeyFromPassword(string password, string salt);
        string MakeKeyFromPasswordBase64(string password, string salt);
        byte[] HashPassword(SymmetricCryptoKey key, string password);
        string HashPasswordBase64(SymmetricCryptoKey key, string password);
    }
}
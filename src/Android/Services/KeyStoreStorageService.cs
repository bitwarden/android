using System.IO;
using System.IO.IsolatedStorage;
using Java.Lang;
using Java.Security;
using Javax.Crypto;
using Android.OS;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    [System.Obsolete]
    public class KeyStoreStorageService : ISecureStorageService
    {
        private const string StorageFile = "Bit.Android.KeyStoreStorageService";

        private static readonly object SaveLock = new object();

        private readonly KeyStore _keyStore;
        private readonly KeyStore.PasswordProtection _protection;

        public KeyStoreStorageService()
            : this(Build.Serial.ToCharArray()) { }

        public KeyStoreStorageService(char[] password)
        {
            _keyStore = KeyStore.GetInstance(KeyStore.DefaultType);
            _protection = new KeyStore.PasswordProtection(password);

            if(File.FileExists(StorageFile))
            {
                using(var stream = new IsolatedStorageFileStream(StorageFile, FileMode.Open, FileAccess.Read, File))
                {
                    _keyStore.Load(stream, password);
                }
            }
            else
            {
                _keyStore.Load(null, password);
            }
        }

        private static IsolatedStorageFile File
        {
            get { return IsolatedStorageFile.GetUserStoreForApplication(); }
        }

        public void Store(string key, byte[] dataBytes)
        {
            _keyStore.SetEntry(key, new KeyStore.SecretKeyEntry(new SecureData(dataBytes)), _protection);
            Save();
        }

        public byte[] Retrieve(string key)
        {
            var entry = _keyStore.GetEntry(key, _protection) as KeyStore.SecretKeyEntry;
            if(entry == null)
            {
                return null;
            }

            return entry.SecretKey.GetEncoded();
        }

        public void Delete(string key)
        {
            _keyStore.DeleteEntry(key);
            Save();
        }

        public bool Contains(string key)
        {
            return _keyStore.ContainsAlias(key);
        }

        private void Save()
        {
            lock(SaveLock)
            {
                using(var stream = new IsolatedStorageFileStream(StorageFile, FileMode.OpenOrCreate, FileAccess.Write, File))
                {
                    _keyStore.Store(stream, _protection.GetPassword());
                }
            }
        }

        private class SecureData : Object, ISecretKey
        {
            private const string Raw = "RAW";

            private readonly byte[] _data;

            public SecureData(byte[] dataBytes)
            {
                _data = dataBytes;
            }

            public string Algorithm
            {
                get { return Raw; }
            }

            public string Format
            {
                get { return Raw; }
            }

            public byte[] GetEncoded()
            {
                return _data;
            }
        }
    }
}
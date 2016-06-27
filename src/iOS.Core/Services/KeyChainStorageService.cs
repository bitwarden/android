using System;
using System.Runtime.CompilerServices;
using Bit.App.Abstractions;
using Foundation;
using Security;

namespace Bit.iOS.Core.Services
{
    public class KeyChainStorageService : ISecureStorageService
    {
        public void Store(string key, byte[] dataBytes)
        {
            using(var data = NSData.FromArray(dataBytes))
            using(var newRecord = GetKeyRecord(key, data))
            {
                Delete(key);
                CheckError(SecKeyChain.Add(newRecord));
            }
        }

        public byte[] Retrieve(string key)
        {
            SecStatusCode resultCode;

            using(var existingRecord = GetKeyRecord(key))
            using(var record = SecKeyChain.QueryAsRecord(existingRecord, out resultCode))
            {
                if(resultCode == SecStatusCode.ItemNotFound)
                {
                    return null;
                }

                CheckError(resultCode);
                return record.Generic.ToArray();
            }
        }

        public void Delete(string key)
        {
            using(var record = GetExistingRecord(key))
            {
                if(record != null)
                {
                    CheckError(SecKeyChain.Remove(record));
                }
            }
        }

        public bool Contains(string key)
        {
            using(var existingRecord = GetExistingRecord(key))
            {
                return existingRecord != null;
            }
        }

        private static void CheckError(SecStatusCode resultCode, [CallerMemberName] string caller = null)
        {
            if(resultCode != SecStatusCode.Success)
            {
                throw new Exception(string.Format("Failed to execute {0}. Result code: {1}", caller, resultCode));
            }
        }

        private static SecRecord GetKeyRecord(string key, NSData data = null)
        {
            var record = new SecRecord(SecKind.GenericPassword)
            {
                Service = "com.8bit.bitwarden",
                Account = key,
                AccessGroup = "LTZ2PFU5D6.com.8bit.bitwarden"
            };

            if(data != null)
            {
                record.Generic = data;
            }

            return record;
        }

        private static SecRecord GetExistingRecord(string key)
        {
            var existingRecord = GetKeyRecord(key);

            SecStatusCode resultCode;
            SecKeyChain.QueryAsRecord(existingRecord, out resultCode);

            return resultCode == SecStatusCode.Success ? existingRecord : null;
        }
    }
}

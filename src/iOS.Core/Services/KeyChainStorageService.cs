using System;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Foundation;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using Security;

namespace Bit.iOS.Core.Services
{
    public class KeyChainStorageService : IStorageService
    {
        private readonly string _keyFormat = "bwKeyChainStorage:{0}";
        private readonly string _service;
        private readonly string _group;
        private readonly JsonSerializerSettings _jsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver()
        };

        public KeyChainStorageService(string service, string group)
        {
            _service = service;
            _group = group;
        }

        public Task<T> GetAsync<T>(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            byte[] dataBytes = null;
            using(var existingRecord = GetKeyRecord(formattedKey))
            using(var record = SecKeyChain.QueryAsRecord(existingRecord, out SecStatusCode resultCode))
            {
                if(resultCode == SecStatusCode.ItemNotFound)
                {
                    return Task.FromResult((T)(object)null);
                }

                CheckError(resultCode);
                dataBytes = record.Generic.ToArray();
            }

            var dataString = Encoding.UTF8.GetString(dataBytes);
            if(typeof(T) == typeof(string))
            {
                return Task.FromResult((T)(object)dataString);
            }
            else
            {
                return Task.FromResult(JsonConvert.DeserializeObject<T>(dataString, _jsonSettings));
            }
        }

        public async Task SaveAsync<T>(string key, T obj)
        {
            if(obj == null)
            {
                await RemoveAsync(key);
                return;
            }

            string dataString = null;
            if(typeof(T) == typeof(string))
            {
                dataString = obj as string;
            }
            else
            {
                dataString = JsonConvert.SerializeObject(obj, _jsonSettings);
            }

            var formattedKey = string.Format(_keyFormat, key);
            var dataBytes = Encoding.UTF8.GetBytes(dataString);
            using(var data = NSData.FromArray(dataBytes))
            using(var newRecord = GetKeyRecord(formattedKey, data))
            {
                await RemoveAsync(key);
                CheckError(SecKeyChain.Add(newRecord));
            }
        }

        public Task RemoveAsync(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            using(var record = GetExistingRecord(formattedKey))
            {
                if(record != null)
                {
                    CheckError(SecKeyChain.Remove(record));
                }
            }
            return Task.FromResult(0);
        }

        private SecRecord GetKeyRecord(string key, NSData data = null)
        {
            var record = new SecRecord(SecKind.GenericPassword)
            {
                Service = _service,
                Account = key,
                AccessGroup = _group
            };
            if(data != null)
            {
                record.Generic = data;
            }
            return record;
        }

        private SecRecord GetExistingRecord(string key)
        {
            var existingRecord = GetKeyRecord(key);
            SecKeyChain.QueryAsRecord(existingRecord, out SecStatusCode resultCode);
            return resultCode == SecStatusCode.Success ? existingRecord : null;
        }

        private void CheckError(SecStatusCode resultCode, [CallerMemberName] string caller = null)
        {
            if(resultCode != SecStatusCode.Success)
            {
                throw new Exception(string.Format("Failed to execute {0}. Result code: {1}", caller, resultCode));
            }
        }
    }
}

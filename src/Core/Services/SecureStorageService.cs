using Bit.Core.Abstractions;
using Bit.Core.Services;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace Bit.App.Services
{
    public class SecureStorageService : IStorageService
    {
        private readonly string _keyFormat = "bwSecureStorage:{0}";
        private readonly JsonSerializerSettings _jsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver()
        };

        public async Task<T> GetAsync<T>(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            var val = await LegacySecureStorage.GetAsync(formattedKey);
            if (typeof(T) == typeof(string))
            {
                return (T)(object)val;
            }
            else
            {
                return JsonConvert.DeserializeObject<T>(val, _jsonSettings);
            }
        }

        public async Task SaveAsync<T>(string key, T obj)
        {
            if (obj == null)
            {
                await RemoveAsync(key);
                return;
            }
            var formattedKey = string.Format(_keyFormat, key);
            if (typeof(T) == typeof(string))
            {
                await LegacySecureStorage.SetAsync(formattedKey, obj as string);
            }
            else
            {
                await LegacySecureStorage.SetAsync(formattedKey,
                    JsonConvert.SerializeObject(obj, _jsonSettings));
            }
        }

        public Task RemoveAsync(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            LegacySecureStorage.Remove(formattedKey);
            return Task.FromResult(0);
        }
    }
}

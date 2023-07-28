using System.Threading.Tasks;
using Bit.Core.Abstractions;
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
            var val = await Microsoft.Maui.Storage.SecureStorage.GetAsync(formattedKey);
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
                await Microsoft.Maui.Storage.SecureStorage.SetAsync(formattedKey, obj as string);
            }
            else
            {
                await Microsoft.Maui.Storage.SecureStorage.SetAsync(formattedKey,
                    JsonConvert.SerializeObject(obj, _jsonSettings));
            }
        }

        public Task RemoveAsync(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            Microsoft.Maui.Storage.SecureStorage.Remove(formattedKey);
            return Task.FromResult(0);
        }
    }
}

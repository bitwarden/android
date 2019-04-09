using Bit.Core.Abstractions;
using Newtonsoft.Json;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SecureStorageService : IStorageService
    {
        private string _keyFormat = "bwSecureStorage:{0}";

        public async Task<T> GetAsync<T>(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            var val = await Xamarin.Essentials.SecureStorage.GetAsync(formattedKey);
            var objType = typeof(T);
            if(objType == typeof(string))
            {
                return (T)(object)val;
            }
            else
            {
                return JsonConvert.DeserializeObject<T>(val);
            }
        }

        public async Task SaveAsync<T>(string key, T obj)
        {
            if(obj == null)
            {
                await RemoveAsync(key);
                return;
            }
            var formattedKey = string.Format(_keyFormat, key);
            var objType = typeof(T);
            if(objType == typeof(string))
            {
                await Xamarin.Essentials.SecureStorage.SetAsync(formattedKey, obj as string);
            }
            else
            {
                await Xamarin.Essentials.SecureStorage.SetAsync(formattedKey, JsonConvert.SerializeObject(obj));
            }
        }

        public Task RemoveAsync(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            Xamarin.Essentials.SecureStorage.Remove(formattedKey);
            return Task.FromResult(0);
        }
    }
}

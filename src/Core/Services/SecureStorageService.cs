using Bit.Core.Abstractions;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SecureStorageService : IStorageService
    {
        private string _keyFormat = "bwSecureStorage:{0}";

        public async Task<T> GetAsync<T>(string key)
        {
            var objType = typeof(T);
            if(objType == typeof(string))
            {
                var formattedKey = string.Format(_keyFormat, key);
                var val = await Xamarin.Essentials.SecureStorage.GetAsync(formattedKey);
                return (T)(object)val;
            }
            else
            {
                throw new Exception("Unsupported object type for secure storage.");
            }
        }

        public async Task SaveAsync<T>(string key, T obj)
        {
            if(obj == null)
            {
                await RemoveAsync(key);
                return;
            }

            var objType = typeof(T);
            if(objType == typeof(string))
            {
                var formattedKey = string.Format(_keyFormat, key);
                await Xamarin.Essentials.SecureStorage.SetAsync(formattedKey, obj as string);
            }
            else
            {
                throw new Exception("Unsupported object type for secure storage.");
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

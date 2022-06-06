using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Newtonsoft.Json;

namespace Bit.Core.Services
{
    public class InMemoryStorageService : IStorageService
    {
        private readonly Dictionary<string, string> _dict = new Dictionary<string, string>();

        public Task<T> GetAsync<T>(string key)
        {
            if (!_dict.ContainsKey(key))
            {
                return Task.FromResult(default(T));
            }
            return Task.FromResult(JsonConvert.DeserializeObject<T>(_dict[key]));
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if (obj == null)
            {
                return RemoveAsync(key);
            }
            _dict.Add(key, JsonConvert.SerializeObject(obj));
            return Task.FromResult(0);
        }

        public Task RemoveAsync(string key)
        {
            if (_dict.ContainsKey(key))
            {
                _dict.Remove(key);
            }
            return Task.FromResult(0);
        }
    }
}

using Bit.Core.Abstractions;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class StateService : IStateService
    {
        private readonly Dictionary<string, object> _state = new Dictionary<string, object>();

        public Task<T> GetAsync<T>(string key)
        {
            return Task.FromResult(_state.ContainsKey(key) ? (T)_state[key] : (T)(object)null);
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if (_state.ContainsKey(key))
            {
                _state[key] = obj;
            }
            else
            {
                _state.Add(key, obj);
            }
            return Task.FromResult(0);
        }

        public Task RemoveAsync(string key)
        {
            if (_state.ContainsKey(key))
            {
                _state.Remove(key);
            }
            return Task.FromResult(0);
        }

        public Task PurgeAsync()
        {
            _state.Clear();
            return Task.FromResult(0);
        }
    }
}

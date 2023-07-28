using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace Bit.App.Services
{
    public class PreferencesStorageService : IStorageService, ISynchronousStorageService
    {
        public static string KeyFormat = "bwPreferencesStorage:{0}";

        private readonly string _sharedName;
        private readonly JsonSerializerSettings _jsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver(),
            NullValueHandling = NullValueHandling.Ignore
        };

        public PreferencesStorageService(string sharedName)
        {
            _sharedName = sharedName;
        }

        public Task<T> GetAsync<T>(string key) => Task.FromResult(Get<T>(key));

        public Task SaveAsync<T>(string key, T obj)
        {
            Save(key, obj);
            return Task.CompletedTask;
        }

        public Task RemoveAsync(string key)
        {
            Remove(key);
            return Task.CompletedTask;
        }

        public T Get<T>(string key)
        {
            var formattedKey = string.Format(KeyFormat, key);
            if (!Microsoft.Maui.Storage.Preferences.ContainsKey(formattedKey, _sharedName))
            {
                return default(T);
            }

            var objType = typeof(T);
            if (objType == typeof(string))
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(string), _sharedName);
                return (T)(object)val;
            }
            else if (objType == typeof(bool) || objType == typeof(bool?))
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(bool), _sharedName);
                return ChangeType<T>(val);
            }
            else if (objType == typeof(int) || objType == typeof(int?))
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(int), _sharedName);
                return ChangeType<T>(val);
            }
            else if (objType == typeof(long) || objType == typeof(long?))
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(long), _sharedName);
                return ChangeType<T>(val);
            }
            else if (objType == typeof(double) || objType == typeof(double?))
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(double), _sharedName);
                return ChangeType<T>(val);
            }
            else if (objType == typeof(DateTime) || objType == typeof(DateTime?))
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(DateTime), _sharedName);
                return ChangeType<T>(val);
            }
            else
            {
                var val = Microsoft.Maui.Storage.Preferences.Get(formattedKey, default(string), _sharedName);
                return JsonConvert.DeserializeObject<T>(val, _jsonSettings);
            }
        }

        public void Save<T>(string key, T obj)
        {
            if (obj == null)
            {
                Remove(key);
                return;
            }

            var formattedKey = string.Format(KeyFormat, key);
            var objType = typeof(T);
            if (objType == typeof(string))
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, obj as string, _sharedName);
            }
            else if (objType == typeof(bool) || objType == typeof(bool?))
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, (obj as bool?).Value, _sharedName);
            }
            else if (objType == typeof(int) || objType == typeof(int?))
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, (obj as int?).Value, _sharedName);
            }
            else if (objType == typeof(long) || objType == typeof(long?))
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, (obj as long?).Value, _sharedName);
            }
            else if (objType == typeof(double) || objType == typeof(double?))
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, (obj as double?).Value, _sharedName);
            }
            else if (objType == typeof(DateTime) || objType == typeof(DateTime?))
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, (obj as DateTime?).Value, _sharedName);
            }
            else
            {
                Microsoft.Maui.Storage.Preferences.Set(formattedKey, JsonConvert.SerializeObject(obj, _jsonSettings),
                    _sharedName);
            }
        }

        public void Remove(string key)
        {
            var formattedKey = string.Format(KeyFormat, key);
            if (Microsoft.Maui.Storage.Preferences.ContainsKey(formattedKey, _sharedName))
            {
                Microsoft.Maui.Storage.Preferences.Remove(formattedKey, _sharedName);
            }
        }

        private static T ChangeType<T>(object value)
        {
            var t = typeof(T);
            if (t.IsGenericType && t.GetGenericTypeDefinition().Equals(typeof(Nullable<>)))
            {
                if (value == null)
                {
                    return default(T);
                }
                t = Nullable.GetUnderlyingType(t);
            }
            return (T)Convert.ChangeType(value, t);
        }
    }
}

using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace Bit.App.Services
{
    public class PreferencesStorageService : IStorageService
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

        public Task<T> GetAsync<T>(string key)
        {
            var formattedKey = string.Format(KeyFormat, key);
            if (!Xamarin.Essentials.Preferences.ContainsKey(formattedKey, _sharedName))
            {
                return Task.FromResult(default(T));
            }

            var objType = typeof(T);
            if (objType == typeof(string))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(string), _sharedName);
                return Task.FromResult((T)(object)val);
            }
            else if (objType == typeof(bool) || objType == typeof(bool?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(bool), _sharedName);
                return Task.FromResult(ChangeType<T>(val));
            }
            else if (objType == typeof(int) || objType == typeof(int?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(int), _sharedName);
                return Task.FromResult(ChangeType<T>(val));
            }
            else if (objType == typeof(long) || objType == typeof(long?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(long), _sharedName);
                return Task.FromResult(ChangeType<T>(val));
            }
            else if (objType == typeof(double) || objType == typeof(double?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(double), _sharedName);
                return Task.FromResult(ChangeType<T>(val));
            }
            else if (objType == typeof(DateTime) || objType == typeof(DateTime?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(DateTime), _sharedName);
                return Task.FromResult(ChangeType<T>(val));
            }
            else
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(string), _sharedName);
                return Task.FromResult(JsonConvert.DeserializeObject<T>(val, _jsonSettings));
            }
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if (obj == null)
            {
                return RemoveAsync(key);
            }

            var formattedKey = string.Format(KeyFormat, key);
            var objType = typeof(T);
            if (objType == typeof(string))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, obj as string, _sharedName);
            }
            else if (objType == typeof(bool) || objType == typeof(bool?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as bool?).Value, _sharedName);
            }
            else if (objType == typeof(int) || objType == typeof(int?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as int?).Value, _sharedName);
            }
            else if (objType == typeof(long) || objType == typeof(long?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as long?).Value, _sharedName);
            }
            else if (objType == typeof(double) || objType == typeof(double?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as double?).Value, _sharedName);
            }
            else if (objType == typeof(DateTime) || objType == typeof(DateTime?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as DateTime?).Value, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, JsonConvert.SerializeObject(obj, _jsonSettings),
                    _sharedName);
            }
            return Task.FromResult(0);
        }

        public Task RemoveAsync(string key)
        {
            var formattedKey = string.Format(KeyFormat, key);
            if (Xamarin.Essentials.Preferences.ContainsKey(formattedKey, _sharedName))
            {
                Xamarin.Essentials.Preferences.Remove(formattedKey, _sharedName);
            }
            return Task.FromResult(0);
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

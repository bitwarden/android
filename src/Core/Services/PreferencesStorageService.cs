using Bit.Core.Abstractions;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class PreferencesStorageService : IStorageService
    {
        private string _keyFormat = "bwPreferencesStorage:{0}";

        public Task<T> GetAsync<T>(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            if(!Xamarin.Essentials.Preferences.ContainsKey(formattedKey))
            {
                return Task.FromResult(default(T));
            }

            var objType = typeof(T);
            if(objType == typeof(string))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(string));
                return Task.FromResult((T)(object)val);
            }
            else if(objType == typeof(bool) || objType == typeof(bool?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(bool));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(int) || objType == typeof(int?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(int));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(long) || objType == typeof(long?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(long));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(double) || objType == typeof(double?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(double));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(DateTime) || objType == typeof(DateTime?))
            {
                var val = Xamarin.Essentials.Preferences.Get(formattedKey, default(DateTime));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else
            {
                throw new Exception("Unsupported object type for preferences.");
            }
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if(obj == null)
            {
                return RemoveAsync(key);
            }

            var formattedKey = string.Format(_keyFormat, key);
            var objType = typeof(T);
            if(objType == typeof(string))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, obj as string);
            }
            else if(objType == typeof(bool) || objType == typeof(bool?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as bool?).Value);
            }
            else if(objType == typeof(int) || objType == typeof(int?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as int?).Value);
            }
            else if(objType == typeof(long) || objType == typeof(long?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as long?).Value);
            }
            else if(objType == typeof(double) || objType == typeof(double?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as double?).Value);
            }
            else if(objType == typeof(DateTime) || objType == typeof(DateTime?))
            {
                Xamarin.Essentials.Preferences.Set(formattedKey, (obj as DateTime?).Value);
            }
            else
            {
                throw new Exception("Unsupported object type for preferences.");
            }
            return Task.FromResult(0);
        }

        public Task RemoveAsync(string key)
        {
            var formattedKey = string.Format(_keyFormat, key);
            if(Xamarin.Essentials.Preferences.ContainsKey(formattedKey))
            {
                Xamarin.Essentials.Preferences.Remove(formattedKey);
            }
            return Task.FromResult(0);
        }
    }
}

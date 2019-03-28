using Bit.Core.Abstractions;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class PreferencesStorageService : IStorageService
    {
        private string _keyFormat = "bwPref:{0}";

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
                var val = Xamarin.Essentials.Preferences.Get(key, default(string));
                return Task.FromResult((T)(object)val);
            }
            else if(objType == typeof(int))
            {
                var val = Xamarin.Essentials.Preferences.Get(key, default(int));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(long))
            {
                var val = Xamarin.Essentials.Preferences.Get(key, default(long));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(double))
            {
                var val = Xamarin.Essentials.Preferences.Get(key, default(double));
                return Task.FromResult((T)Convert.ChangeType(val, objType));
            }
            else if(objType == typeof(DateTime))
            {
                var val = Xamarin.Essentials.Preferences.Get(key, default(DateTime));
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

            var objType = typeof(T);
            if(objType == typeof(string))
            {
                Xamarin.Essentials.Preferences.Set(key, obj as string);
            }
            else if(objType == typeof(int))
            {
                Xamarin.Essentials.Preferences.Set(key, (obj as int?).Value);
            }
            else if(objType == typeof(long))
            {
                Xamarin.Essentials.Preferences.Set(key, (obj as long?).Value);
            }
            else if(objType == typeof(double))
            {
                Xamarin.Essentials.Preferences.Set(key, (obj as double?).Value);
            }
            else if(objType == typeof(DateTime))
            {
                Xamarin.Essentials.Preferences.Set(key, (obj as DateTime?).Value);
            }
            else
            {
                throw new Exception("Unsupported object type for preferences.");
            }
            return Task.FromResult(0);
        }

        public Task RemoveAsync(string key)
        {
            if(Xamarin.Essentials.Preferences.ContainsKey(key))
            {
                Xamarin.Essentials.Preferences.Remove(key);
            }
            return Task.FromResult(0);
        }
    }
}

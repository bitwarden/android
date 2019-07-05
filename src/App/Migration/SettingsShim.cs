using System;

namespace Bit.App.Migration
{
    public class SettingsShim
    {
        private readonly string _sharedName;

        public SettingsShim(string sharedName = null)
        {
            _sharedName = sharedName;
        }

        public bool Contains(string key)
        {
            return _sharedName != null ? Xamarin.Essentials.Preferences.ContainsKey(key, _sharedName) :
                Xamarin.Essentials.Preferences.ContainsKey(key);
        }

        public string GetValueOrDefault(string key, string defaultValue)
        {
            return _sharedName != null ? Xamarin.Essentials.Preferences.Get(key, defaultValue, _sharedName) :
                Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public DateTime GetValueOrDefault(string key, DateTime defaultValue)
        {
            return _sharedName != null ? Xamarin.Essentials.Preferences.Get(key, defaultValue, _sharedName) :
                Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public bool GetValueOrDefault(string key, bool defaultValue)
        {
            return _sharedName != null ? Xamarin.Essentials.Preferences.Get(key, defaultValue, _sharedName) :
                Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public int GetValueOrDefault(string key, int defaultValue)
        {
            return _sharedName != null ? Xamarin.Essentials.Preferences.Get(key, defaultValue, _sharedName) :
                Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public long GetValueOrDefault(string key, long defaultValue)
        {
            return _sharedName != null ? Xamarin.Essentials.Preferences.Get(key, defaultValue, _sharedName) :
                Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public void AddOrUpdateValue(string key, string value)
        {
            if(_sharedName != null)
            {
                Xamarin.Essentials.Preferences.Set(key, value, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Set(key, value);
            }
        }

        public void AddOrUpdateValue(string key, DateTime value)
        {
            if(_sharedName != null)
            {
                Xamarin.Essentials.Preferences.Set(key, value, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Set(key, value);
            }
        }

        public void AddOrUpdateValue(string key, bool value)
        {
            if(_sharedName != null)
            {
                Xamarin.Essentials.Preferences.Set(key, value, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Set(key, value);
            }
        }

        public void AddOrUpdateValue(string key, long value)
        {
            if(_sharedName != null)
            {
                Xamarin.Essentials.Preferences.Set(key, value, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Set(key, value);
            }
        }

        public void AddOrUpdateValue(string key, int value)
        {
            if(_sharedName != null)
            {
                Xamarin.Essentials.Preferences.Set(key, value, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Set(key, value);
            }
        }

        public void Remove(string key)
        {
            if(_sharedName != null)
            {
                Xamarin.Essentials.Preferences.Remove(key, _sharedName);
            }
            else
            {
                Xamarin.Essentials.Preferences.Remove(key);
            }
        }
    }
}

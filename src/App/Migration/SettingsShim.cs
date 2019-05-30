using System;

namespace Bit.App.Migration
{
    public class SettingsShim
    {
        public bool Contains(string key)
        {
            return Xamarin.Essentials.Preferences.ContainsKey(key);
        }

        public string GetValueOrDefault(string key, string defaultValue)
        {
            return Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public DateTime GetValueOrDefault(string key, DateTime defaultValue)
        {
            return Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public bool GetValueOrDefault(string key, bool defaultValue)
        {
            return Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public int GetValueOrDefault(string key, int defaultValue)
        {
            return Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public long GetValueOrDefault(string key, long defaultValue)
        {
            return Xamarin.Essentials.Preferences.Get(key, defaultValue);
        }

        public void AddOrUpdateValue(string key, string value)
        {
            Xamarin.Essentials.Preferences.Set(key, value);
        }

        public void AddOrUpdateValue(string key, DateTime value)
        {
            Xamarin.Essentials.Preferences.Set(key, value);
        }

        public void AddOrUpdateValue(string key, bool value)
        {
            Xamarin.Essentials.Preferences.Set(key, value);
        }

        public void AddOrUpdateValue(string key, long value)
        {
            Xamarin.Essentials.Preferences.Set(key, value);
        }

        public void AddOrUpdateValue(string key, int value)
        {
            Xamarin.Essentials.Preferences.Set(key, value);
        }

        public void Remove(string key)
        {
            Xamarin.Essentials.Preferences.Remove(key);
        }
    }
}

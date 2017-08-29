using System;
using Foundation;
#if __IOS__
using UIKit;
#endif
using Plugin.Settings.Abstractions;

namespace Bit.iOS.Core.Services
{
    /// <summary>
    /// Main implementation for ISettings
    /// </summary>
    [Preserve(AllMembers = true)]
    public class Settings : ISettings
    {
        private readonly object locker = new object();
        private readonly string _defaultsName;

        public Settings(string defaultsName)
        {
            _defaultsName = defaultsName;
        }

        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <typeparam name="T">Vaue of t (bool, int, float, long, string)</typeparam>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        T GetValueOrDefaultInternal<T>(string key, T defaultValue = default(T), string fileName = null)
        {
            lock(locker)
            {
                var defaults = GetUserDefaults(fileName);

                if(defaults[key] == null)
                    return defaultValue;

                Type typeOf = typeof(T);
                if(typeOf.IsGenericType && typeOf.GetGenericTypeDefinition() == typeof(Nullable<>))
                {
                    typeOf = Nullable.GetUnderlyingType(typeOf);
                }
                object value = null;
                var typeCode = Type.GetTypeCode(typeOf);
                switch(typeCode)
                {
                    case TypeCode.Decimal:
                        var savedDecimal = defaults.StringForKey(key);
                        value = Convert.ToDecimal(savedDecimal, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case TypeCode.Boolean:
                        value = defaults.BoolForKey(key);
                        break;
                    case TypeCode.Int64:
                        var savedInt64 = defaults.StringForKey(key);
                        value = Convert.ToInt64(savedInt64, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case TypeCode.Double:
                        value = defaults.DoubleForKey(key);
                        break;
                    case TypeCode.String:
                        value = defaults.StringForKey(key);
                        break;
                    case TypeCode.Int32:
                        value = (Int32)defaults.IntForKey(key);
                        break;
                    case TypeCode.Single:
                        value = defaults.FloatForKey(key);
                        break;

                    case TypeCode.DateTime:
                        var savedTime = defaults.StringForKey(key);
                        if(string.IsNullOrWhiteSpace(savedTime))
                        {
                            value = defaultValue;
                        }
                        else
                        {
                            var ticks = Convert.ToInt64(savedTime, System.Globalization.CultureInfo.InvariantCulture);
                            if(ticks >= 0)
                            {
                                //Old value, stored before update to UTC values
                                value = new DateTime(ticks);
                            }
                            else
                            {
                                //New value, UTC
                                value = new DateTime(-ticks, DateTimeKind.Utc);
                            }
                        }
                        break;
                    default:

                        if(defaultValue is Guid)
                        {
                            var outGuid = Guid.Empty;
                            var savedGuid = defaults.StringForKey(key);
                            if(string.IsNullOrWhiteSpace(savedGuid))
                            {
                                value = outGuid;
                            }
                            else
                            {
                                Guid.TryParse(savedGuid, out outGuid);
                                value = outGuid;
                            }
                        }
                        else
                        {
                            throw new ArgumentException($"Value of type {typeCode} is not supported.");
                        }

                        break;
                }


                return null != value ? (T)value : defaultValue;
            }
        }

        /// <summary>
        /// Adds or updates a value
        /// </summary>
        /// <param name="key">key to update</param>
        /// <param name="value">value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True if added or update and you need to save</returns>
        bool AddOrUpdateValueInternal<T>(string key, T value, string fileName = null)
        {
            if(value == null)
            {
                Remove(key, fileName);
                return true;
            }

            Type typeOf = typeof(T);
            if(typeOf.IsGenericType && typeOf.GetGenericTypeDefinition() == typeof(Nullable<>))
            {
                typeOf = Nullable.GetUnderlyingType(typeOf);
            }
            var typeCode = Type.GetTypeCode(typeOf);
            return AddOrUpdateValueCore(key, value, typeCode, fileName);
        }

        bool AddOrUpdateValueCore(string key, object value, TypeCode typeCode, string fileName)
        {
            lock(locker)
            {
                var defaults = GetUserDefaults(fileName);
                switch(typeCode)
                {
                    case TypeCode.Decimal:
                        defaults.SetString(Convert.ToString(value, System.Globalization.CultureInfo.InvariantCulture), key);
                        break;
                    case TypeCode.Boolean:
                        defaults.SetBool(Convert.ToBoolean(value), key);
                        break;
                    case TypeCode.Int64:
                        defaults.SetString(Convert.ToString(value, System.Globalization.CultureInfo.InvariantCulture), key);
                        break;
                    case TypeCode.Double:
                        defaults.SetDouble(Convert.ToDouble(value, System.Globalization.CultureInfo.InvariantCulture), key);
                        break;
                    case TypeCode.String:
                        defaults.SetString(Convert.ToString(value), key);
                        break;
                    case TypeCode.Int32:
                        defaults.SetInt(Convert.ToInt32(value, System.Globalization.CultureInfo.InvariantCulture), key);
                        break;
                    case TypeCode.Single:
                        defaults.SetFloat(Convert.ToSingle(value, System.Globalization.CultureInfo.InvariantCulture), key);
                        break;
                    case TypeCode.DateTime:
                        defaults.SetString(Convert.ToString(-(Convert.ToDateTime(value)).ToUniversalTime().Ticks), key);
                        break;
                    default:
                        if(value is Guid)
                        {
                            if(value == null)
                                value = Guid.Empty;

                            defaults.SetString(((Guid)value).ToString(), key);
                        }
                        else
                        {
                            throw new ArgumentException($"Value of type {typeCode} is not supported.");
                        }
                        break;
                }
                try
                {
                    defaults.Synchronize();
                }
                catch(Exception ex)
                {
                    Console.WriteLine("Unable to save: " + key, " Message: " + ex.Message);
                }
            }


            return true;
        }

        /// <summary>
        /// Removes a desired key from the settings
        /// </summary>
        /// <param name="key">Key for setting</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        public void Remove(string key, string fileName = null)
        {
            lock(locker)
            {
                var defaults = GetUserDefaults(fileName);
                try
                {
                    if(defaults[key] != null)
                    {
                        defaults.RemoveObject(key);
                        defaults.Synchronize();
                    }
                }
                catch(Exception ex)
                {
                    Console.WriteLine("Unable to remove: " + key, " Message: " + ex.Message);
                }
            }
        }

        /// <summary>
        /// Clear all keys from settings
        /// </summary>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        public void Clear(string fileName = null)
        {
            lock(locker)
            {
                var defaults = GetUserDefaults(fileName);
                try
                {
                    var items = defaults.ToDictionary();

                    foreach(var item in items.Keys)
                    {
                        if(item is NSString nsString)
                            defaults.RemoveObject(nsString);
                    }
                    defaults.Synchronize();
                }
                catch(Exception ex)
                {
                    Console.WriteLine("Unable to clear all defaults. Message: " + ex.Message);
                }
            }
        }

        /// <summary>
        /// Checks to see if the key has been added.
        /// </summary>
        /// <param name="key">Key to check</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True if contains key, else false</returns>
        public bool Contains(string key, string fileName = null)
        {
            lock(locker)
            {
                var defaults = GetUserDefaults(fileName);
                try
                {
                    var setting = defaults[key];
                    return setting != null;
                }
                catch(Exception ex)
                {
                    Console.WriteLine("Unable to clear all defaults. Message: " + ex.Message);
                }

                return false;
            }
        }

        NSUserDefaults GetUserDefaults(string fileName = null)
        {
            if(string.IsNullOrWhiteSpace(fileName) && !string.IsNullOrWhiteSpace(_defaultsName))
            {
                return new NSUserDefaults(_defaultsName, NSUserDefaultsType.SuiteName);
            }

            return string.IsNullOrWhiteSpace(fileName) ?
                NSUserDefaults.StandardUserDefaults :
                new NSUserDefaults(fileName, NSUserDefaultsType.SuiteName);
        }



        #region GetValueOrDefault
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public decimal GetValueOrDefault(string key, decimal defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public bool GetValueOrDefault(string key, bool defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public long GetValueOrDefault(string key, long defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public string GetValueOrDefault(string key, string defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public int GetValueOrDefault(string key, int defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public float GetValueOrDefault(string key, float defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public DateTime GetValueOrDefault(string key, DateTime defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public Guid GetValueOrDefault(string key, Guid defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        /// <summary>
        /// Gets the current value or the default that you specify.
        /// </summary>
        /// <param name="key">Key for settings</param>
        /// <param name="defaultValue">default value if not set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>Value or default</returns>
        public double GetValueOrDefault(string key, double defaultValue, string fileName = null) =>
            GetValueOrDefaultInternal(key, defaultValue, fileName);
        #endregion

        #region AddOrUpdateValue
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, decimal value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, bool value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, long value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, string value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, int value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, float value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, DateTime value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, Guid value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);
        /// <summary>
        /// Adds or updates the value 
        /// </summary>
        /// <param name="key">Key for settting</param>
        /// <param name="value">Value to set</param>
        /// <param name="fileName">Name of file for settings to be stored and retrieved </param>
        /// <returns>True of was added or updated and you need to save it.</returns>
        public bool AddOrUpdateValue(string key, double value, string fileName = null) =>
            AddOrUpdateValueInternal(key, value, fileName);

        #endregion


        /// <summary>
        /// Attempts to open the app settings page.
        /// </summary>
        /// <returns>true if success, else false and not supported</returns>
        public bool OpenAppSettings()
        {
#if __IOS__
            //Opening settings only open in iOS 8+
            if(!UIDevice.CurrentDevice.CheckSystemVersion(8, 0))
                return false;

            try
            {
                UIApplication.SharedApplication.OpenUrl(new NSUrl(UIApplication.OpenSettingsUrlString));
                return true;
            }
            catch
            {
                return false;
            }
#else
            return false;
#endif
        }

    }

}
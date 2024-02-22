#nullable enable
#if ANDROID || IOS

namespace Bit.Core.Services;

public class LegacySecureStorage
{
    internal static readonly string Alias = $"{AppInfo.PackageName}.xamarinessentials";

    public static Task<string> GetAsync(string key)
    {
        if (string.IsNullOrWhiteSpace(key))
            throw new ArgumentNullException(nameof(key));

        string result = null;

#if ANDROID
        object locker = new object();
        string? encVal = Preferences.Get(key, null, Alias);

        if (!string.IsNullOrEmpty(encVal))
        {
            byte[] encData = Convert.FromBase64String(encVal);
            lock (locker)
            {
                AndroidKeyStore keyStore = new AndroidKeyStore(Platform.AppContext, Alias, false);
                result = keyStore.Decrypt(encData);
            }
        }
#elif IOS
        KeyChain keyChain = new KeyChain();
        result = keyChain.ValueForKey(key, Alias);
#endif
        return Task.FromResult(result);
    }

    public static Task SetAsync(string key, string value)
    {
#if ANDROID
        var context = Platform.AppContext;

        byte[] encryptedData = null;
        object locker = new object();
        lock (locker)
        {
            AndroidKeyStore keyStore = new AndroidKeyStore(Platform.AppContext, Alias, false);
            encryptedData = keyStore.Encrypt(value);
        }

        var encStr = Convert.ToBase64String(encryptedData);
        Preferences.Set(key, encStr, Alias);
#elif IOS
        KeyChain keyChain = new KeyChain();
        result = keyChain.SetValueForKey(value, key, Alias);
#endif
        return Task.CompletedTask;
    }

    public static bool Remove(string key)
    {
        bool result = false;

#if ANDROID
        Preferences.Remove(key, Alias);
        result = true;
#elif IOS
        KeyChain keyChain = new KeyChain();
        result = keyChain.Remove(key, Alias);
#endif
        return result;
    }
    
    public static void RemoveAll()
    {
#if ANDROID
        Preferences.Clear(Alias);
#elif IOS
        KeyChain keyChain = new KeyChain();
        keyChain.RemoveAll(Alias);
#endif
    }
}
#endif

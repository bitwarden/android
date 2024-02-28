#nullable enable

#if IOS
using Security;
#endif

#if ANDROID
using Javax.Crypto;
#endif

namespace Bit.Core.Services;

public class LegacySecureStorage
{
    internal static readonly string Alias = $"{AppInfo.PackageName}.xamarinessentials";

#if IOS
    private static SecAccessible DefaultAccessible { get; set; } = SecAccessible.AfterFirstUnlock;
#endif


    public static Task<string?> GetAsync(string key)
    {
        if (string.IsNullOrWhiteSpace(key))
            throw new ArgumentNullException(nameof(key));

#if ANDROID
        return Task.Run(() =>
        {
            object locker = new object();
            string? encVal = Preferences.Get(key, null, Alias);

            if (!string.IsNullOrEmpty(encVal))
            {
                try
                {
                    byte[] encData = Convert.FromBase64String(encVal);
                    lock (locker)
                    {
                        AndroidKeyStore keyStore = new AndroidKeyStore(Platform.AppContext, Alias, false);
                        return keyStore.Decrypt(encData);
                    }
                }
                catch (AEADBadTagException)
                {
                    System.Diagnostics.Debug.WriteLine($"Unable to decrypt key, {key}, which is likely due to an app uninstall. Removing old key and returning null.");
                    Remove(key);
                }
            }

            return null;
        });
#elif IOS
        var keyChain = new KeyChain(DefaultAccessible);
        return Task.FromResult<string?>(keyChain.ValueForKey(key, Alias));
#else
        return Task.FromResult((string?)null);
#endif
    }

    public static Task SetAsync(string key, string value)
    {
#if ANDROID
        return Task.Run(() =>
        {
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
        });
#elif IOS
        KeyChain keyChain = new KeyChain(DefaultAccessible);
        keyChain.SetValueForKey(value, key, Alias);
#endif
        return Task.CompletedTask;
    }

    public static bool Remove(string key)
    {
#if ANDROID
        Preferences.Remove(key, Alias);
        return true;
#elif IOS
        var keyChain = new KeyChain(DefaultAccessible);
        return keyChain.Remove(key, Alias);
#else
        return false;
#endif
    }
    
    public static void RemoveAll()
    {
#if ANDROID
        Preferences.Clear(Alias);
#elif IOS
        var keyChain = new KeyChain(DefaultAccessible);
        keyChain.RemoveAll(Alias);
#endif
    }
}

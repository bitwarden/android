using Android.App;
using Android.Content;
using Android.OS;
using Android.Provider;
using Android.Service.QuickSettings;
using Bit.App.Utilities;

namespace Bit.App.Droid.Utilities
{
    public static class AndroidHelpers
    {
        private static string BaseEnvironmentUrlRestrictionKey = "baseEnvironmentUrl";

        public static string GetFileName(Context context, Android.Net.Uri uri)
        {
            string name = null;
            string[] projection = { MediaStore.MediaColumns.DisplayName };
            var metaCursor = context.ContentResolver.Query(uri, projection, null, null, null);
            if (metaCursor != null)
            {
                try
                {
                    if (metaCursor.MoveToFirst())
                    {
                        name = metaCursor.GetString(0);
                    }
                }
                finally
                {
                    metaCursor.Close();
                }
            }
            return name;
        }

        public static async Task SetPreconfiguredRestrictionSettingsAsync(Context context)
        {
            var restrictionsManager = (RestrictionsManager)context.GetSystemService(Context.RestrictionsService);
            var restrictions = restrictionsManager.ApplicationRestrictions;
            var dict = new Dictionary<string, string>();
            if (restrictions.ContainsKey(BaseEnvironmentUrlRestrictionKey))
            {
                dict.Add(BaseEnvironmentUrlRestrictionKey, restrictions.GetString(BaseEnvironmentUrlRestrictionKey));
            }

            if (dict.Count > 0)
            {
                await AppHelpers.SetPreconfiguredSettingsAsync(dict);
            }
        }

        public static PendingIntentFlags AddPendingIntentMutabilityFlag(PendingIntentFlags pendingIntentFlags, bool isMutable)
        {
            //Mutable flag was added on API level 31
            if (isMutable && Build.VERSION.SdkInt >= BuildVersionCodes.S)
            {
                return pendingIntentFlags | PendingIntentFlags.Mutable;
            }

            //Immutable flag was added on API level 23
            if (!isMutable && Build.VERSION.SdkInt >= BuildVersionCodes.M)
            {
                return pendingIntentFlags | PendingIntentFlags.Immutable;
            }

            return pendingIntentFlags;
        }

        public static void StartActivityAndCollapseWithIntent(this TileService service, Intent intent, bool isMutable)
        {
            //For Android 14+ We need to use PendingIntent instead of Intent directly. Older versions still need to use Intent.
            if (Build.VERSION.SdkInt < BuildVersionCodes.UpsideDownCake)
            {
                service.StartActivityAndCollapse(intent);
                return;
            }
            var pendingIntent = PendingIntent.GetActivity(
                service.ApplicationContext,
                0,
                intent,
                AddPendingIntentMutabilityFlag(PendingIntentFlags.UpdateCurrent, isMutable)
            );
            if (pendingIntent == null) 
            { 
                return; 
            }
            service.StartActivityAndCollapse(pendingIntent);
        }
    }
}

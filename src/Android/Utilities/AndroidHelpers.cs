using System.Collections.Generic;
using System.Threading.Tasks;
using Android.Content;
using Android.Provider;
using Bit.App.Utilities;

namespace Bit.Droid.Utilities
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
    }
}

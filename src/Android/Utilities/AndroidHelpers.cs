using Android.Content;
using Android.Provider;

namespace Bit.Droid.Utilities
{
    public static class AndroidHelpers
    {
        public static string GetFileName(Context context, Android.Net.Uri uri)
        {
            string name = null;
            string[] projection = { MediaStore.MediaColumns.DisplayName };
            var metaCursor = context.ContentResolver.Query(uri, projection, null, null, null);
            if(metaCursor != null)
            {
                try
                {
                    if(metaCursor.MoveToFirst())
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
    }
}

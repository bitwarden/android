using Android.Content;
using Android.OS;

namespace Bit.Droid.Utilities
{
    public static class IntentExtensions
    {
        public static void Validate(this Intent intent)
        {
            try
            {
                // Check if getting the bundle of the extras causes any exception when unparcelling
                // Note: getting the bundle like this will cause to call unparcel() internally
                var b = intent?.Extras?.GetBundle("trashstringwhichhasnousebuttocheckunparcel");
            }
            catch (BadParcelableException)
            {
                intent.ReplaceExtras((Bundle)null);
            }
        }
    }
}

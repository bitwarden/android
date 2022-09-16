using Android.App;
using Android.Content.PM;
using Android.OS;
using Bit.Droid.Utilities;

namespace Bit.Droid
{
    [Activity(
        NoHistory = true, 
        LaunchMode = LaunchMode.SingleTop,
        Exported = true)]
    [IntentFilter(new[] { Android.Content.Intent.ActionView },
        Categories = new[] { Android.Content.Intent.CategoryDefault, Android.Content.Intent.CategoryBrowsable },
        DataScheme = "bitwarden")]
    public class WebAuthCallbackActivity : Xamarin.Essentials.WebAuthenticatorCallbackActivity
    {
        protected override void OnCreate(Bundle savedInstanceState)
        {
            Intent?.Validate();
            base.OnCreate(savedInstanceState);
        }
    }
}

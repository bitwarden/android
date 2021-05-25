using Android.App;
using Android.Content.PM;

namespace Bit.Droid
{
    [Activity(
        NoHistory = true, 
        LaunchMode = LaunchMode.SingleTop)]
    [IntentFilter(new[] { Android.Content.Intent.ActionView },
        Categories = new[] { Android.Content.Intent.CategoryDefault, Android.Content.Intent.CategoryBrowsable },
        DataScheme = "bitwarden")]
    public class WebAuthCallbackActivity : Xamarin.Essentials.WebAuthenticatorCallbackActivity { }
}

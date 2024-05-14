using Android.App;
using Android.Content.PM;
using Android.OS;
using Android.Runtime;
using Bit.App.Droid.Utilities;

namespace Bit.Droid
{
    [Register("com.x8bit.bitwarden.WebAuthCallbackActivity")]
    public class WebAuthCallbackActivity : WebAuthenticatorCallbackActivity
    {
        protected override void OnCreate(Bundle savedInstanceState)
        {
            Intent?.Validate();
            base.OnCreate(savedInstanceState);
        }
    }
}

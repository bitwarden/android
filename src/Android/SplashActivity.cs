using Android.App;
using Android.Content.PM;
using Android.Runtime;
using Android.OS;
using Android.Support.V7.App;
using System.Threading.Tasks;
using Android.Content;
using Bit.App.Utilities;

namespace Bit.Droid
{
    [Activity(
        Label = "Bitwarden",
        MainLauncher = true,
        NoHistory = true,
        Icon = "@mipmap/ic_launcher",
        Theme = "@style/MainTheme.Splash",
        WindowSoftInputMode = Android.Views.SoftInput.StateHidden,
        ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    [Register("com.x8bit.bitwarden.SplashActivity")]
    public class SplashActivity : AppCompatActivity
    {
        public override void OnCreate(Bundle savedInstanceState, PersistableBundle persistentState)
        {
            UpdateTheme(ThemeManager.GetTheme());
            base.OnCreate(savedInstanceState, persistentState);
        }

        protected override void OnResume()
        {
            base.OnResume();
            var startupWork = new Task(() =>
            {
                var mainIntent = new Intent(Application.Context, typeof(MainActivity));
                mainIntent.PutExtra("myVaultTile", Intent.GetBooleanExtra("myVaultTile", false));
                StartActivity(mainIntent);
            });
            startupWork.Start();
        }

        private void UpdateTheme(string theme)
        {
            if(theme == "dark")
            {
                SetTheme(Resource.Style.DarkTheme_Splash);
            }
            else
            {
                SetTheme(Resource.Style.MainTheme_Splash);
            }
        }
    }
}

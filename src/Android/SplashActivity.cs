using System;
using Android.App;
using Android.OS;
using Android.Content;
using Android.Support.V7.App;
using System.Threading.Tasks;

namespace Bit.Android
{
    [Activity(Theme = "@style/BitwardenTheme.Splash", 
        MainLauncher = true, 
        NoHistory = true,
        WindowSoftInputMode = global::Android.Views.SoftInput.StateHidden)]
    public class SplashActivity : AppCompatActivity
    {
        public override void OnCreate(Bundle savedInstanceState, PersistableBundle persistentState)
        {
            base.OnCreate(savedInstanceState, persistentState);
        }

        protected override void OnResume()
        {
            base.OnResume();
            var startupWork = new Task(() =>
            {
                StartActivity(new Intent(Application.Context, typeof(MainActivity)));
            });

            startupWork.Start();
        }
    }
}

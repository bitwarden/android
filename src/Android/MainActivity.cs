using System;

using Android.App;
using Android.Content.PM;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using Bit.App.Abstractions;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using Acr.UserDialogs;
using PushNotification.Plugin.Abstractions;

namespace Bit.Android
{
    [Activity(Label = "bitwarden", Icon = "@drawable/icon", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    public class MainActivity : global::Xamarin.Forms.Platform.Android.FormsApplicationActivity
    {
        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);
            Console.WriteLine("A OnCreate");

            HockeyApp.Android.CrashManager.Register(this);
            global::Xamarin.Forms.Forms.Init(this, bundle);

            LoadApplication(new App.App(
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IUserDialogs>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                Resolver.Resolve<IFingerprint>(),
                Resolver.Resolve<ISettings>(),
                Resolver.Resolve<IPushNotification>(),
                Resolver.Resolve<ILockService>(),
                Resolver.Resolve<IGoogleAnalyticsService>()));
        }

        protected override void OnPause()
        {
            Console.WriteLine("A OnPause");
            base.OnPause();
        }

        protected override void OnDestroy()
        {
            Console.WriteLine("A OnDestroy");
            base.OnDestroy();
        }

        protected override void OnRestart()
        {
            Console.WriteLine("A OnRestart");
            base.OnRestart();
        }

        protected override void OnStart()
        {
            Console.WriteLine("A OnStart");
            base.OnStart();
        }

        protected override void OnStop()
        {
            Console.WriteLine("A OnStop");
            base.OnStop();
        }

        protected override void OnResume()
        {
            Console.WriteLine("A OnResume");
            base.OnResume();
        }
    }
}

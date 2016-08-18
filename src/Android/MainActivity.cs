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
using Android.Content;

namespace Bit.Android
{
    [Activity(Label = "bitwarden", Icon = "@drawable/icon", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    public class MainActivity : global::Xamarin.Forms.Platform.Android.FormsApplicationActivity
    {
        private const string HockeyAppId = "d3834185b4a643479047b86c65293d42";

        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);
            Console.WriteLine("A OnCreate");

            var appIdService = Resolver.Resolve<IAppIdService>();
            var authService = Resolver.Resolve<IAuthService>();

            HockeyApp.Android.CrashManager.Register(this, HockeyAppId,
                new HockeyAppCrashManagerListener(appIdService, authService));
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

            Xamarin.Forms.MessagingCenter.Subscribe<Xamarin.Forms.Application>(
                Xamarin.Forms.Application.Current, "RateApp", (sender) =>
            {
                RateApp();
            });
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

        public void RateApp()
        {
            try
            {
                var rateIntent = RateIntentForUrl("market://details");
                StartActivity(rateIntent);
            }
            catch(ActivityNotFoundException)
            {
                var rateIntent = RateIntentForUrl("https://play.google.com/store/apps/details");
                StartActivity(rateIntent);
            }
        }

        private Intent RateIntentForUrl(string url)
        {
            var intent = new Intent(Intent.ActionView, global::Android.Net.Uri.Parse($"{url}?id={PackageName}"));
            var flags = ActivityFlags.NoHistory | ActivityFlags.MultipleTask;
            if((int)Build.VERSION.SdkInt >= 21)
            {
                flags |= ActivityFlags.NewDocument;
            }
            else
            {
                // noinspection deprecation
                flags |= ActivityFlags.ClearWhenTaskReset;
            }

            intent.AddFlags(flags);
            return intent;
        }
    }
}

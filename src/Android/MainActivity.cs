using System;

using Android.App;
using Android.Content.PM;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using Microsoft.Practices.Unity;
using Bit.App.Abstractions;
using Bit.App.Services;
using XLabs.Ioc.Unity;
using XLabs.Ioc;
using Bit.Android.Services;

namespace Bit.Android
{
    [Activity(Label = "bitwarden", Icon = "@drawable/icon", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    public class MainActivity : global::Xamarin.Forms.Platform.Android.FormsApplicationActivity
    {
        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);

            global::Xamarin.Forms.Forms.Init(this, bundle);

            if(!Resolver.IsSet)
            {
                SetIoc();
            }

            LoadApplication(new App.App(Resolver.Resolve<IAuthService>(), Resolver.Resolve<IDatabaseService>()));
        }

        private void SetIoc()
        {
            var container = new UnityContainer();

            container
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISecureStorageService, KeyStoreStorageService>(new ContainerControlledLifetimeManager())
                .RegisterType<IApiService, ApiService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
        }
    }
}

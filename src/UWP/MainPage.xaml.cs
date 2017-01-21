using System;
using Bit.App.Abstractions;
using XLabs.Ioc;
using Plugin.Fingerprint.Abstractions;
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using Acr.UserDialogs;
using System.Reflection;
using Xamarin.Forms.Platform.UWP;
using Xamarin.Forms;
using System.Threading.Tasks;
using Windows.UI.Xaml;
using NUnit.Framework.Api;
using Microsoft.HockeyApp;

namespace Bit.UWP
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage
    {
        private const string HockeyAppId = "5628855c469d43d2aa8e520664a45036";
        public MainPage()
        {
            this.InitializeComponent();
           
            LoadApplication(new Bit.App.App(
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IUserDialogs>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                Resolver.Resolve<IFingerprint>(),
                Resolver.Resolve<ISettings>(),
                Resolver.Resolve<ILockService>(),
                Resolver.Resolve<IGoogleAnalyticsService>(),
                Resolver.Resolve<ILocalizeService>()));
        }

      

       
    }
}

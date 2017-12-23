using Acr.UserDialogs;
using Bit.App.Abstractions;
using Plugin.Connectivity.Abstractions;
using Plugin.Settings.Abstractions;
using Xamarin.Forms.Platform.UWP;
using XLabs.Ioc;

namespace Bit.UWP
{
    public sealed partial class MainPage : WindowsPage
    {
        public MainPage()
        {
            InitializeComponent();
            LoadApplication(new Bit.App.App(
                null,
                Resolver.Resolve<IAuthService>(),
                Resolver.Resolve<IConnectivity>(),
                Resolver.Resolve<IDatabaseService>(),
                Resolver.Resolve<ISyncService>(),
                Resolver.Resolve<ISettings>(),
                Resolver.Resolve<ILockService>(),
                Resolver.Resolve<ILocalizeService>(),
                Resolver.Resolve<IAppInfoService>(),
                Resolver.Resolve<IAppSettingsService>(),
                Resolver.Resolve<IDeviceActionService>()));
        }
    }
}

using Acr.UserDialogs;
using Bit.App.Abstractions;
using Plugin.Connectivity.Abstractions;
using Plugin.Settings.Abstractions;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using Xamarin.Forms.Platform.UWP;
using XLabs.Ioc;

// The Blank Page item template is documented at https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace Bit.UWP
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : WindowsPage
    {
        public MainPage()
        {
            this.InitializeComponent();
            LoadApplication(new Bit.App.App(
    null,
    false,
    Resolver.Resolve<IAuthService>(),
    Resolver.Resolve<IConnectivity>(),
    Resolver.Resolve<IUserDialogs>(),
    Resolver.Resolve<IDatabaseService>(),
    Resolver.Resolve<ISyncService>(),
    Resolver.Resolve<ISettings>(),
    Resolver.Resolve<ILockService>(),
    Resolver.Resolve<IGoogleAnalyticsService>(),
    Resolver.Resolve<ILocalizeService>(),
    Resolver.Resolve<IAppInfoService>(),
    Resolver.Resolve<IAppSettingsService>(),
    Resolver.Resolve<IDeviceActionService>()));

        }
    }
}

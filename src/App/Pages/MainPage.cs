using System;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        public MainPage()
        {
            BarTintColor = Color.FromHex("222d32");
            TintColor = Color.FromHex("ffffff");

            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage());
            var vaultNavigation = new ExtendedNavigationPage(new VaultListPage());
            var syncNavigation = new ExtendedNavigationPage(new SyncPage());

            vaultNavigation.Title = AppResources.MyVault;
            vaultNavigation.Icon = "fa-lock";

            syncNavigation.Title = AppResources.Sync;
            syncNavigation.Icon = "fa-refresh";

            settingsNavigation.Title = AppResources.Settings;
            settingsNavigation.Icon = "fa-cogs";

            Children.Add(vaultNavigation);
            Children.Add(syncNavigation);
            Children.Add(settingsNavigation);
        }
    }
}

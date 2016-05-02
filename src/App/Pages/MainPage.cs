using System;
using Bit.App.Abstractions;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : TabbedPage
    {
        public MainPage()
        {
            var vaultNavigation = new NavigationPage(new VaultListPage());
            vaultNavigation.BarBackgroundColor = Color.FromHex("3c8dbc");
            vaultNavigation.BarTextColor = Color.FromHex("ffffff");
            vaultNavigation.Title = "My Vault";

            var settingsNavigation = new NavigationPage(new SettingsPage());
            settingsNavigation.BarBackgroundColor = Color.FromHex("3c8dbc");
            settingsNavigation.BarTextColor = Color.FromHex("ffffff");
            settingsNavigation.Title = "Settings";

            Children.Add(vaultNavigation);
            Children.Add(new SyncPage());
            Children.Add(settingsNavigation);
        }
    }
}

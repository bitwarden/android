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
            var favoritesNavigation = new ExtendedNavigationPage(new VaultListSitesPage(true));
            var vaultNavigation = new ExtendedNavigationPage(new VaultListSitesPage(false));
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage());

            favoritesNavigation.Title = AppResources.Favorites;
            favoritesNavigation.Icon = "star";

            vaultNavigation.Title = AppResources.MyVault;
            vaultNavigation.Icon = "fa-lock";

            toolsNavigation.Title = AppResources.Tools;
            toolsNavigation.Icon = "wrench";

            settingsNavigation.Title = AppResources.Settings;
            settingsNavigation.Icon = "cogs";

            Children.Add(favoritesNavigation);
            Children.Add(vaultNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);
        }
    }
}

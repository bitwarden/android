using System;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        public MainPage(string uri = null, bool myVault = false)
        {
            TintColor = Color.FromHex("3c8dbc");

            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage());
            var favoritesNavigation = new ExtendedNavigationPage(new VaultListCiphersPage(true, uri));
            var vaultNavigation = new ExtendedNavigationPage(new VaultListCiphersPage(false, uri));
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage());

            favoritesNavigation.Title = AppResources.Favorites;
            favoritesNavigation.Icon = "star.png";

            vaultNavigation.Title = AppResources.MyVault;
            vaultNavigation.Icon = "fa_lock.png";

            toolsNavigation.Title = AppResources.Tools;
            toolsNavigation.Icon = "tools.png";

            settingsNavigation.Title = AppResources.Settings;
            settingsNavigation.Icon = "cogs.png";

            Children.Add(favoritesNavigation);
            Children.Add(vaultNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);

            if(myVault || uri != null)
            {
                SelectedItem = vaultNavigation;
            }
        }
    }
}

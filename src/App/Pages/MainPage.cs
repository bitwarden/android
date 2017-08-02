using System;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        public MainPage(string uri = null)
        {
            TintColor = Color.FromHex("3c8dbc");

            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage());
            var favoritesNavigation = new ExtendedNavigationPage(new VaultListLoginsPage(true, uri));
            var vaultNavigation = new ExtendedNavigationPage(new VaultListLoginsPage(false, uri));
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage());

            favoritesNavigation.Title = AppResources.Favorites;
            favoritesNavigation.Icon = "star";

            vaultNavigation.Title = AppResources.MyVault;
            vaultNavigation.Icon = "fa_lock";

            toolsNavigation.Title = AppResources.Tools;
            toolsNavigation.Icon = "tools";

            settingsNavigation.Title = AppResources.Settings;
            settingsNavigation.Icon = "cogs";

            Children.Add(favoritesNavigation);
            Children.Add(vaultNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);

            if(uri != null)
            {
                SelectedItem = vaultNavigation;
            }
        }
    }
}

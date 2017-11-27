using System;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        public MainPage(bool myVault = false)
        {
            TintColor = Color.FromHex("3c8dbc");

            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage());
            var favoritesNavigation = new ExtendedNavigationPage(new VaultListCiphersPage(favorites: true));
            var vaultNavigation = new ExtendedNavigationPage(new VaultListGroupingsPage());
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage());

            favoritesNavigation.Icon = "star.png";
            vaultNavigation.Icon = "fa_lock.png";
            toolsNavigation.Icon = "tools.png";
            settingsNavigation.Icon = "cogs.png";

            Children.Add(favoritesNavigation);
            Children.Add(vaultNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);

            if(myVault)
            {
                SelectedItem = vaultNavigation;
            }
        }
    }
}

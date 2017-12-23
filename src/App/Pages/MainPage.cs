using System;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        public MainPage()
        {
            TintColor = Color.FromHex("3c8dbc");

            var vaultNavigation = new ExtendedNavigationPage(new VaultListGroupingsPage());
            var passwordGeneratorNavigation = new ExtendedNavigationPage(new ToolsPasswordGeneratorPage());
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage());
            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage());

            vaultNavigation.Icon = "fa_lock.png";
            passwordGeneratorNavigation.Icon = "refresh.png";
            toolsNavigation.Icon = "tools.png";
            settingsNavigation.Icon = "cogs.png";

            Children.Add(vaultNavigation);
            Children.Add(passwordGeneratorNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);

            SelectedItem = vaultNavigation;
        }
    }
}

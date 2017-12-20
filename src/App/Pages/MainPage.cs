using System;
using Bit.App.Controls;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        public MainPage(bool myVault = false)
        {
            TintColor = Color.FromHex("3c8dbc");

            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage());
            var vaultNavigation = new ExtendedNavigationPage(new VaultListGroupingsPage());
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage());

            vaultNavigation.Icon = "fa_lock.png";
            toolsNavigation.Icon = "tools.png";
            settingsNavigation.Icon = "cogs.png";

            Children.Add(vaultNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);

            if(myVault || Resolver.Resolve<IAppSettingsService>().DefaultPageVault)
            {
                SelectedItem = vaultNavigation;
            }
        }
    }
}

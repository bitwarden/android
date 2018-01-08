using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class MainPage : ExtendedTabbedPage
    {
        private ExtendedNavigationPage _vaultPage;

        public MainPage()
        {
            TintColor = Color.FromHex("3c8dbc");

            _vaultPage = new ExtendedNavigationPage(new VaultListGroupingsPage());
            var passwordGeneratorNavigation = new ExtendedNavigationPage(new ToolsPasswordGeneratorPage(this));
            var toolsNavigation = new ExtendedNavigationPage(new ToolsPage(this));
            var settingsNavigation = new ExtendedNavigationPage(new SettingsPage(this));

            _vaultPage.Icon = "fa_lock.png";
            passwordGeneratorNavigation.Icon = "refresh.png";
            toolsNavigation.Icon = "tools.png";
            settingsNavigation.Icon = "cogs.png";

            Children.Add(_vaultPage);
            Children.Add(passwordGeneratorNavigation);
            Children.Add(toolsNavigation);
            Children.Add(settingsNavigation);
        }

        public void ResetToVaultPage()
        {
            CurrentPage = _vaultPage;
        }
    }
}

using Bit.App.Effect;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class TabsPage : TabbedPage
    {
        private NavigationPage _groupingsPage;
        private NavigationPage _generatorPage;

        public TabsPage(AppOptions appOptions = null)
        {
            _groupingsPage = new NavigationPage(new GroupingsPage(true))
            {
                Title = AppResources.MyVault,
                Icon = "lock.png"
            };
            Children.Add(_groupingsPage);

            _generatorPage = new NavigationPage(new GeneratorPage(true, null, this))
            {
                Title = AppResources.Generator,
                Icon = "refresh.png"
            };
            Children.Add(_generatorPage);

            var settingsPage = new NavigationPage(new SettingsPage(this))
            {
                Title = AppResources.Settings,
                Icon = "cog.png"
            };
            Children.Add(settingsPage);

            if(Device.RuntimePlatform == Device.Android)
            {
                Effects.Add(new TabBarEffect());

                Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetToolbarPlacement(this,
                    Xamarin.Forms.PlatformConfiguration.AndroidSpecific.ToolbarPlacement.Bottom);
                Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetIsSwipePagingEnabled(this, false);
                Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetIsSmoothScrollEnabled(this, false);
                Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetBarSelectedItemColor(this,
                    (Color)Application.Current.Resources["TabBarSelectedItemColor"]);
                Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetBarItemColor(this,
                    (Color)Application.Current.Resources["TabBarItemColor"]);
            }

            if(appOptions?.GeneratorTile ?? false)
            {
                appOptions.GeneratorTile = false;
                ResetToGeneratorPage();
            }
            else if(appOptions?.MyVaultTile ?? false)
            {
                appOptions.MyVaultTile = false;
            }
        }

        public void ResetToVaultPage()
        {
            CurrentPage = _groupingsPage;
        }

        public void ResetToGeneratorPage()
        {
            CurrentPage = _generatorPage;
        }

        protected async override void OnCurrentPageChanged()
        {
            if(CurrentPage is NavigationPage navPage)
            {
                if(navPage.RootPage is GroupingsPage groupingsPage)
                {
                    // Load something?
                }
                else if(navPage.RootPage is GeneratorPage genPage)
                {
                    await genPage.InitAsync();
                }
                else if(navPage.RootPage is SettingsPage settingsPage)
                {
                    await settingsPage.InitAsync();
                }
            }
        }
    }
}

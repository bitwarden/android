using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class TabsPage : TabbedPage
    {
        public TabsPage()
        {
            var groupingsPage = new NavigationPage(new GroupingsPage(true))
            {
                Title = AppResources.MyVault,
                Icon = "lock.png"
            };
            Children.Add(groupingsPage);

            var generatorPage = new NavigationPage(new GeneratorPage(true, null))
            {
                Title = AppResources.Generator,
                Icon = "refresh.png"
            };
            Children.Add(generatorPage);

            var settingsPage = new NavigationPage(new SettingsPage())
            {
                Title = AppResources.Settings,
                Icon = "cogs.png"
            };
            Children.Add(settingsPage);

            Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetToolbarPlacement(this,
                Xamarin.Forms.PlatformConfiguration.AndroidSpecific.ToolbarPlacement.Bottom);
            Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetIsSwipePagingEnabled(this, false);
            Xamarin.Forms.PlatformConfiguration.AndroidSpecific.TabbedPage.SetIsSmoothScrollEnabled(this, false);
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
                    // Load something?
                }
            }
        }
    }
}

using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Abstractions;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SettingsAboutPage : ExtendedContentPage
    {
        private readonly IAppInfoService _appInfoService;

        public SettingsAboutPage()
        {
            _appInfoService = Resolver.Resolve<IAppInfoService>();
            Init();
        }

        public void Init()
        {
            var logo = new Image
            {
                Source = "logo",
                HorizontalOptions = LayoutOptions.Center
            };

            var versionLabel = new Label
            {
                Text = $@"Version {_appInfoService.Version} ({_appInfoService.Build})
© 8bit Solutions LLC 2015-{DateTime.Now.Year}",
                HorizontalTextAlignment = TextAlignment.Center
            };

            var logoVersionStackLayout = new StackLayout
            {
                Children = { logo, versionLabel },
                Spacing = 20,
                Padding = new Thickness(0, 40)
            };

            var creditsCell = new ExtendedTextCell
            {
                Text = "Credits",
                ShowDisclousure = true
            };
            creditsCell.Tapped += RateCell_Tapped;

            var table = new ExtendedTableView
            {
                VerticalOptions = LayoutOptions.Start,
                EnableScrolling = false,
                NoHeader = true,
                Intent = TableIntent.Menu,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        creditsCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;
            }

            var stackLayout = new StackLayout
            {
                Children = { logoVersionStackLayout, table },
                Spacing = 0
            };

            Title = "About bitwarden";
            Content = new ScrollView { Content = stackLayout };
            NavigationPage.SetBackButtonTitle(this, "About");
        }

        private void RateCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsCreditsPage());
        }
    }
}

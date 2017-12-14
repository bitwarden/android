using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Abstractions;
using XLabs.Ioc;
using Bit.App.Resources;
using FFImageLoading.Forms;
using Bit.App.Utilities;

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

        public ExtendedTextCell CreditsCell { get; set; }

        public void Init()
        {
            var logo = new CachedImage
            {
                Source = "logo.png",
                HorizontalOptions = LayoutOptions.Center,
                WidthRequest = 282,
                HeightRequest = 44
            };

            var versionLabel = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                Text = $@"{AppResources.Version} {_appInfoService.Version} ({_appInfoService.Build})
© 8bit Solutions LLC 2015-{DateTime.Now.Year}",
                HorizontalTextAlignment = TextAlignment.Center
            };

            var logoVersionStackLayout = new StackLayout
            {
                Children = { logo, versionLabel },
                Spacing = 20,
                Padding = new Thickness(0, 40, 0, 0)
            };

            CreditsCell = new ExtendedTextCell
            {
                Text = AppResources.Credits,
                ShowDisclousure = true
            };

            var table = new ExtendedTableView
            {
                VerticalOptions = LayoutOptions.Start,
                EnableScrolling = false,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        CreditsCell
                    }
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;
            }

            var stackLayout = new StackLayout
            {
                Children = { logoVersionStackLayout, table },
                Spacing = 0
            };

            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.Windows)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.About;
            Content = new ScrollView { Content = stackLayout };
        }

        private void RateCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsCreditsPage());
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            CreditsCell.Tapped += RateCell_Tapped;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            CreditsCell.Tapped -= RateCell_Tapped;
        }
    }
}

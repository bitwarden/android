using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class ToolsPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public ToolsPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ToolsViewCell GeneratorCell { get; set; }
        public ToolsViewCell WebCell { get; set; }
        public ToolsViewCell ShareCell { get; set; }
        public ToolsViewCell ImportCell { get; set; }
        public ToolsViewCell ExtensionCell { get; set; }
        public ToolsViewCell AutofillCell { get; set; }

        public void Init()
        {
            GeneratorCell = new ToolsViewCell(AppResources.PasswordGenerator, AppResources.PasswordGeneratorDescription,
                "refresh");
            WebCell = new ToolsViewCell(AppResources.WebVault, AppResources.WebVaultDescription, "globe");
            ShareCell = new ToolsViewCell(AppResources.ShareVault, AppResources.ShareVaultDescription, "share_tools");
            ImportCell = new ToolsViewCell(AppResources.ImportLogins, AppResources.ImportLoginsDescription, "cloudup");

            var section = new TableSection(" ") { GeneratorCell };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ExtensionCell = new ToolsViewCell(AppResources.BitwardenAppExtension,
                    AppResources.BitwardenAppExtensionDescription, "upload");
                section.Add(ExtensionCell);
            }
            else
            {
                AutofillCell = new ToolsViewCell(AppResources.BitwardenAutofillService,
                    AppResources.BitwardenAutofillServiceDescription, "upload");
                section.Add(AutofillCell);
            }

            section.Add(WebCell);
            section.Add(ShareCell);
            section.Add(ImportCell);

            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    section
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 100;
            }

            Title = AppResources.Tools;
            Content = table;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            GeneratorCell.Tapped += GeneratorCell_Tapped;
            WebCell.Tapped += WebCell_Tapped;
            ShareCell.Tapped += ShareCell_Tapped;
            ImportCell.Tapped += ImportCell_Tapped;
            if(ExtensionCell != null)
            {
                ExtensionCell.Tapped += ExtensionCell_Tapped;
            }
            if(AutofillCell != null)
            {
                AutofillCell.Tapped += AutofillCell_Tapped;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            GeneratorCell.Tapped -= GeneratorCell_Tapped;
            WebCell.Tapped -= WebCell_Tapped;
            ShareCell.Tapped -= ShareCell_Tapped;
            ImportCell.Tapped -= ImportCell_Tapped;
            if(ExtensionCell != null)
            {
                ExtensionCell.Tapped -= ExtensionCell_Tapped;
            }
            if(AutofillCell != null)
            {
                AutofillCell.Tapped -= AutofillCell_Tapped;
            }

        }

        private void AutofillCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new ToolsAutofillServicePage()));
        }

        private void ExtensionCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new ToolsExtensionPage()));
        }

        private async void GeneratorCell_Tapped(object sender, EventArgs e)
        {
            await Navigation.PushForDeviceAsync(new ToolsPasswordGeneratorPage());
        }

        private void WebCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("OpenedTool", "Web");
            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        private void ShareCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("OpenedTool", "Share");
            Device.OpenUri(new Uri("https://vault.bitwarden.com/#/?org=free"));
        }

        private async void ImportCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(AppResources.ImportLoginsConfirmation, null, AppResources.Yes,
                AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedTool", "Import");
            Device.OpenUri(new Uri("https://help.bitwarden.com/article/import-data/"));
        }

        public class ToolsViewCell : ExtendedViewCell
        {
            public ToolsViewCell(string labelText, string detailText, string imageSource)
            {
                var label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    LineBreakMode = LineBreakMode.TailTruncation,
                    Text = labelText
                };

                if(Device.RuntimePlatform == Device.Android)
                {
                    label.TextColor = Color.Black;
                }

                var detail = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    LineBreakMode = LineBreakMode.WordWrap,
                    Style = (Style)Application.Current.Resources["text-muted"],
                    Text = detailText
                };

                var image = new CachedImage
                {
                    Source = imageSource,
                    WidthRequest = 44,
                    HeightRequest = 44
                };

                var grid = new Grid
                {
                    ColumnSpacing = 15,
                    RowSpacing = 0,
                    Padding = new Thickness(15, 20)
                };
                grid.AdjustPaddingForDevice();

                grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Auto) });
                grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Auto) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(44, GridUnitType.Absolute) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                grid.Children.Add(image, 0, 0);
                Grid.SetRowSpan(image, 2);
                grid.Children.Add(label, 1, 0);
                grid.Children.Add(detail, 1, 1);

                ShowDisclousure = true;
                View = grid;
            }
        }
    }
}

using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Abstractions;
using XLabs.Ioc;
using Bit.App.Resources;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class SettingsHelpPage : ExtendedContentPage
    {
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public SettingsHelpPage()
        {
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ExtendedTextCell EmailCell { get; set; }
        public ExtendedTextCell WebsiteCell { get; set; }
        public ExtendedTextCell BugCell { get; set; }
        public RedrawableStackLayout StackLayout { get; set; }
        private CustomLabel EmailLabel { get; set; }
        private CustomLabel WebsiteLabel { get; set; }
        private CustomLabel BugLabel { get; set; }

        public void Init()
        {
            EmailCell = new ExtendedTextCell
            {
                Text = AppResources.EmailUs,
                ShowDisclousure = true
            };

            var emailTable = new CustomTableView(this)
            {
                Root = new TableRoot
                {
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        EmailCell
                    }
                }
            };

            EmailLabel = new CustomLabel(this)
            {
                Text = AppResources.EmailUsDescription
            };

            WebsiteCell = new ExtendedTextCell
            {
                Text = AppResources.VisitOurWebsite,
                ShowDisclousure = true
            };

            var websiteTable = new CustomTableView(this)
            {
                NoHeader = true,
                Root = new TableRoot
                {
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        WebsiteCell
                    }
                }
            };

            WebsiteLabel = new CustomLabel(this)
            {
                Text = AppResources.VisitOurWebsiteDescription
            };

            BugCell = new ExtendedTextCell
            {
                Text = AppResources.FileBugReport,
                ShowDisclousure = true
            };

            var bugTable = new CustomTableView(this)
            {
                NoHeader = true,
                Root = new TableRoot
                {
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        BugCell
                    }
                }
            };

            BugLabel = new CustomLabel(this)
            {
                Text = AppResources.FileBugReportDescription
            };

            StackLayout = new RedrawableStackLayout
            {
                Children = { emailTable, EmailLabel, websiteTable, WebsiteLabel, bugTable, BugLabel },
                Spacing = 0
            };

            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.UWP)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.HelpAndFeedback;
            Content = new ScrollView { Content = StackLayout };
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            EmailCell.Tapped += EmailCell_Tapped;
            WebsiteCell.Tapped += WebsiteCell_Tapped;
            BugCell.Tapped += BugCell_Tapped;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            EmailCell.Tapped -= EmailCell_Tapped;
            WebsiteCell.Tapped -= WebsiteCell_Tapped;
            BugCell.Tapped -= BugCell_Tapped;
        }

        private void EmailCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("HelpEmail");
            Device.OpenUri(new Uri("mailto:hello@bitwarden.com"));
        }

        private void WebsiteCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("HelpWebsite");
            Device.OpenUri(new Uri("https://bitwarden.com/contact/"));
        }

        private void BugCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("HelpBug");
            Device.OpenUri(new Uri("https://github.com/bitwarden/mobile"));
        }

        private class CustomTableView : ExtendedTableView
        {
            public CustomTableView(SettingsHelpPage page)
            {
                Intent = TableIntent.Settings;
                EnableScrolling = false;
                HasUnevenRows = true;
                EnableSelection = true;
                VerticalOptions = LayoutOptions.Start;
                NoFooter = true;
                WrappingStackLayout = () => page.StackLayout;

                if(Device.RuntimePlatform == Device.iOS)
                {
                    RowHeight = -1;
                    EstimatedRowHeight = 44;
                }
            }
        }

        private class CustomLabel : Label
        {
            public CustomLabel(ContentPage page)
            {
                LineBreakMode = LineBreakMode.WordWrap;
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label));
                Style = (Style)Application.Current.Resources["text-muted"];
                Margin = new Thickness(15, (page.IsLandscape() ? 5 : 0), 15, 25);
            }
        }
    }
}

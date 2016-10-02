using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Abstractions;
using XLabs.Ioc;

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

        public void Init()
        {
            var emailCell = new ExtendedTextCell
            {
                Text = "Email Us",
                ShowDisclousure = true
            };
            emailCell.Tapped += EmailCell_Tapped;

            var emailTable = new CustomTableView
            {
                Root = new TableRoot
                {
                    new TableSection
                    {
                        emailCell
                    }
                }
            };

            var emailLabel = new CustomLabel(this)
            {
                Text = "Email us directly to get help or leave feedback."
            };

            var websiteCell = new ExtendedTextCell
            {
                Text = "Visit Our Website",
                ShowDisclousure = true
            };
            websiteCell.Tapped += WebsiteCell_Tapped;

            var websiteTable = new CustomTableView
            {
                NoHeader = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        websiteCell
                    }
                }
            };

            var websiteLabel = new CustomLabel(this)
            {
                Text = "Visit our website to get help, news, email us, and/or learn more about how to use bitwarden."
            };

            var bugCell = new ExtendedTextCell
            {
                Text = "File a Bug Report",
                ShowDisclousure = true
            };
            bugCell.Tapped += BugCell_Tapped;

            var bugTable = new CustomTableView
            {
                NoHeader = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        bugCell
                    }
                }
            };

            var bugLabel = new CustomLabel(this)
            {
                Text = "Open an issue at our GitHub repository."
            };

            var stackLayout = new StackLayout
            {
                Children = { emailTable, emailLabel, websiteTable, websiteLabel, bugTable, bugLabel },
                Spacing = 0
            };

            stackLayout.LayoutChanged += (sender, args) =>
            {
                websiteLabel.WidthRequest = stackLayout.Bounds.Width - websiteLabel.Bounds.Left * 2;
                emailLabel.WidthRequest = stackLayout.Bounds.Width - emailLabel.Bounds.Left * 2;
                bugLabel.WidthRequest = stackLayout.Bounds.Width - bugLabel.Bounds.Left * 2;
            };

            Title = "Help and Feedback";
            Content = new ScrollView { Content = stackLayout };
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
            public CustomTableView()
            {
                Intent = TableIntent.Settings;
                EnableScrolling = false;
                HasUnevenRows = true;
                EnableSelection = true;
                VerticalOptions = LayoutOptions.Start;
                NoFooter = true;

                if(Device.OS == TargetPlatform.iOS)
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

using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Utilities;
using Xamarin.Forms;
using XLabs.Ioc;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class EnvironmentPage : ExtendedContentPage
    {
        private IAppSettingsService _appSettings;
        private IDeviceActionService _deviceActionService;
        private IGoogleAnalyticsService _googleAnalyticsService;

        public EnvironmentPage()
            : base(updateActivity: false)
        {
            _appSettings = Resolver.Resolve<IAppSettingsService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public FormEntryCell BaseUrlCell { get; set; }
        public FormEntryCell WebVaultUrlCell { get; set; }
        public FormEntryCell ApiUrlCell { get; set; }
        public FormEntryCell IdentityUrlCell { get; set; }
        public FormEntryCell IconsUrlCell { get; set; }
        public StackLayout StackLayout { get; set; }
        public Label SelfHostLabel { get; set; }
        public Label CustomLabel { get; set; }

        private void Init()
        {
            MessagingCenter.Send(Application.Current, "ShowStatusBar", true);

            IconsUrlCell = new FormEntryCell(AppResources.IconsUrl, entryKeyboard: Keyboard.Url);
            IconsUrlCell.Entry.Text = _appSettings.IconsUrl;

            IdentityUrlCell = new FormEntryCell(AppResources.IdentityUrl, nextElement: IconsUrlCell.Entry,
                entryKeyboard: Keyboard.Url);
            IdentityUrlCell.Entry.Text = _appSettings.IdentityUrl;

            ApiUrlCell = new FormEntryCell(AppResources.ApiUrl, nextElement: IdentityUrlCell.Entry,
                entryKeyboard: Keyboard.Url);
            ApiUrlCell.Entry.Text = _appSettings.ApiUrl;

            WebVaultUrlCell = new FormEntryCell(AppResources.WebVaultUrl, nextElement: ApiUrlCell.Entry,
                entryKeyboard: Keyboard.Url);
            WebVaultUrlCell.Entry.Text = _appSettings.WebVaultUrl;

            BaseUrlCell = new FormEntryCell(AppResources.ServerUrl, nextElement: WebVaultUrlCell.Entry,
                entryKeyboard: Keyboard.Url);
            BaseUrlCell.Entry.Text = _appSettings.BaseUrl;

            var table = new FormTableView
            {
                Root = new TableRoot
                {
                    new TableSection(AppResources.SelfHostedEnvironment)
                    {
                        BaseUrlCell
                    }
                }
            };

            SelfHostLabel = new Label
            {
                Text = AppResources.SelfHostedEnvironmentFooter,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                Margin = new Thickness(15, (this.IsLandscape() ? 5 : 0), 15, 25)
            };

            var table2 = new FormTableView
            {
                Root = new TableRoot
                {
                    new TableSection(AppResources.CustomEnvironment)
                    {
                        WebVaultUrlCell,
                        ApiUrlCell,
                        IdentityUrlCell,
                        IconsUrlCell
                    }
                }
            };

            CustomLabel = new Label
            {
                Text = AppResources.CustomEnvironmentFooter,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                Margin = new Thickness(15, (this.IsLandscape() ? 5 : 0), 15, 25)
            };

            StackLayout = new StackLayout
            {
                Children = { table, SelfHostLabel, table2, CustomLabel },
                Spacing = 0
            };

            var scrollView = new ScrollView
            {
                Content = StackLayout
            };

            var toolbarItem = new ToolbarItem(AppResources.Save, Helpers.ToolbarImage("envelope.png"), async () => await SaveAsync(),
                ToolbarItemOrder.Default, 0);

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = table2.RowHeight = -1;
                table.EstimatedRowHeight = table2.EstimatedRowHeight = 70;
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close, () =>
                {
                    MessagingCenter.Send(Application.Current, "ShowStatusBar", false);
                }));
            }
            else if(Device.RuntimePlatform == Device.Android)
            {
                table2.BottomPadding = 50;
            }

            ToolbarItems.Add(toolbarItem);
            Title = AppResources.Settings;
            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            MessagingCenter.Send(Application.Current, "ShowStatusBar", true);
            BaseUrlCell.InitEvents();
            IconsUrlCell.InitEvents();
            IdentityUrlCell.InitEvents();
            ApiUrlCell.InitEvents();
            WebVaultUrlCell.InitEvents();
            StackLayout.LayoutChanged += Layout_LayoutChanged;
            BaseUrlCell.Entry.FocusWithDelay();
        }
        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            BaseUrlCell.Dispose();
            IconsUrlCell.Dispose();
            IdentityUrlCell.Dispose();
            ApiUrlCell.Dispose();
            WebVaultUrlCell.Dispose();
            StackLayout.LayoutChanged -= Layout_LayoutChanged;
        }

        private void Layout_LayoutChanged(object sender, EventArgs e)
        {
            SelfHostLabel.WidthRequest = StackLayout.Bounds.Width - SelfHostLabel.Bounds.Left * 2;
            CustomLabel.WidthRequest = StackLayout.Bounds.Width - CustomLabel.Bounds.Left * 2;
        }

        private async Task SaveAsync()
        {
            Uri result;

            if(!string.IsNullOrWhiteSpace(BaseUrlCell.Entry.Text))
            {
                BaseUrlCell.Entry.Text = FixUrl(BaseUrlCell.Entry.Text);
                if(!Uri.TryCreate(BaseUrlCell.Entry.Text, UriKind.Absolute, out result))
                {
                    await DisplayAlert(null, string.Format(AppResources.FormattedIncorrectly, AppResources.ServerUrl),
                        AppResources.Ok);
                    return;
                }
            }
            else
            {
                BaseUrlCell.Entry.Text = null;
            }

            if(!string.IsNullOrWhiteSpace(WebVaultUrlCell.Entry.Text))
            {
                WebVaultUrlCell.Entry.Text = FixUrl(WebVaultUrlCell.Entry.Text);
                if(!Uri.TryCreate(WebVaultUrlCell.Entry.Text, UriKind.Absolute, out result))
                {
                    await DisplayAlert(null, string.Format(AppResources.FormattedIncorrectly, AppResources.WebVaultUrl),
                        AppResources.Ok);
                    return;
                }
            }
            else
            {
                WebVaultUrlCell.Entry.Text = null;
            }

            if(!string.IsNullOrWhiteSpace(ApiUrlCell.Entry.Text))
            {
                ApiUrlCell.Entry.Text = FixUrl(ApiUrlCell.Entry.Text);
                if(!Uri.TryCreate(ApiUrlCell.Entry.Text, UriKind.Absolute, out result))
                {
                    await DisplayAlert(null, string.Format(AppResources.FormattedIncorrectly, AppResources.ApiUrl),
                        AppResources.Ok);
                    return;
                }
            }
            else
            {
                ApiUrlCell.Entry.Text = null;
            }

            if(!string.IsNullOrWhiteSpace(IdentityUrlCell.Entry.Text))
            {
                IdentityUrlCell.Entry.Text = FixUrl(IdentityUrlCell.Entry.Text);
                if(!Uri.TryCreate(IdentityUrlCell.Entry.Text, UriKind.Absolute, out result))
                {
                    await DisplayAlert(null, string.Format(AppResources.FormattedIncorrectly, AppResources.IdentityUrl),
                        AppResources.Ok);
                    return;
                }
            }
            else
            {
                IdentityUrlCell.Entry.Text = null;
            }

            if(!string.IsNullOrWhiteSpace(IconsUrlCell.Entry.Text))
            {
                IconsUrlCell.Entry.Text = FixUrl(IconsUrlCell.Entry.Text);
                if(!Uri.TryCreate(IconsUrlCell.Entry.Text, UriKind.Absolute, out result))
                {
                    await DisplayAlert(null, string.Format(AppResources.FormattedIncorrectly, AppResources.IconsUrl),
                        AppResources.Ok);
                    return;
                }
            }
            else
            {
                IconsUrlCell.Entry.Text = null;
            }

            _appSettings.BaseUrl = BaseUrlCell.Entry.Text;
            _appSettings.IconsUrl = IconsUrlCell.Entry.Text;
            _appSettings.IdentityUrl = IdentityUrlCell.Entry.Text;
            _appSettings.ApiUrl = ApiUrlCell.Entry.Text;
            _appSettings.WebVaultUrl = WebVaultUrlCell.Entry.Text;
            _deviceActionService.Toast(AppResources.EnvironmentSaved);
            _googleAnalyticsService.TrackAppEvent("SetEnvironmentUrls");
            await Navigation.PopForDeviceAsync();
        }

        private string FixUrl(string url)
        {
            url = url.TrimEnd('/');
            if(!url.StartsWith("http://") && !url.StartsWith("https://"))
            {
                url = $"https://{url}";
            }
            return url;
        }

        private class FormTableView : ExtendedTableView
        {
            public FormTableView()
            {
                Intent = TableIntent.Settings;
                EnableScrolling = false;
                HasUnevenRows = true;
                EnableSelection = true;
                VerticalOptions = LayoutOptions.Start;
                NoFooter = true;
            }
        }
    }
}

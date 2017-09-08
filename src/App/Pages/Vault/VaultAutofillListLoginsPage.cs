using System;
using System.Linq;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Utilities;
using System.Threading;
using Bit.App.Models;
using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class VaultAutofillListLoginsPage : ExtendedContentPage
    {
        private readonly ILoginService _loginService;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly IDeviceActionService _clipboardService;
        private readonly ISettingsService _settingsService;
        private CancellationTokenSource _filterResultsCancellationTokenSource;
        private readonly string _name;

        public VaultAutofillListLoginsPage(string uriString)
            : base(true)
        {
            Uri = uriString;

            Uri uri;
            if(uriString?.StartsWith(Constants.AndroidAppProtocol) ?? false)
            {
                _name = uriString.Substring(Constants.AndroidAppProtocol.Length);
            }
            else if(!System.Uri.TryCreate(uriString, UriKind.Absolute, out uri) ||
                !DomainName.TryParseBaseDomain(uri.Host, out _name))
            {
                _name = "--";
            }

            _loginService = Resolver.Resolve<ILoginService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _clipboardService = Resolver.Resolve<IDeviceActionService>();
            _settingsService = Resolver.Resolve<ISettingsService>();
            UserDialogs = Resolver.Resolve<IUserDialogs>();
            GoogleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ExtendedObservableCollection<VaultListPageModel.AutofillGrouping> PresentationLoginsGroup { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.AutofillGrouping>();
        public StackLayout NoDataStackLayout { get; set; }
        public ListView ListView { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }
        private SearchToolBarItem SearchItem { get; set; }
        private AddLoginToolBarItem AddLoginItem { get; set; }
        private IGoogleAnalyticsService GoogleAnalyticsService { get; set; }
        private IUserDialogs UserDialogs { get; set; }
        private string Uri { get; set; }

        private void Init()
        {
            var noDataLabel = new Label
            {
                Text = string.Format(AppResources.NoLoginsForUri, _name ?? "--"),
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            var addLoginButton = new ExtendedButton
            {
                Text = AppResources.AddALogin,
                Command = new Command(() => AddLoginAsync()),
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            NoDataStackLayout = new StackLayout
            {
                Children = { noDataLabel, addLoginButton },
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Padding = new Thickness(20, 0),
                Spacing = 20
            };

            AddLoginItem = new AddLoginToolBarItem(this);
            ToolbarItems.Add(AddLoginItem);
            SearchItem = new SearchToolBarItem(this);
            ToolbarItems.Add(SearchItem);

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationLoginsGroup,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new HeaderViewCell()),
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(
                    (VaultListPageModel.Login l) => MoreClickedAsync(l)))
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ListView.RowHeight = -1;
            }

            Title = string.Format(AppResources.LoginsForUri, _name ?? "--");

            LoadingIndicator = new ActivityIndicator
            {
                IsRunning = true,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            Content = LoadingIndicator;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            ListView.ItemSelected += LoginSelected;
            AddLoginItem.InitEvents();
            SearchItem.InitEvents();
            _filterResultsCancellationTokenSource = FetchAndLoadVault();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            ListView.ItemSelected -= LoginSelected;
            AddLoginItem.Dispose();
            SearchItem.Dispose();
        }

        protected override bool OnBackButtonPressed()
        {
            GoogleAnalyticsService.TrackExtensionEvent("BackClosed", Uri.StartsWith("http") ? "Website" : "App");
            MessagingCenter.Send(Application.Current, "Autofill", (VaultListPageModel.Login)null);
            return true;
        }

        private void AdjustContent()
        {
            if(PresentationLoginsGroup.Count > 0)
            {
                Content = ListView;
            }
            else
            {
                Content = NoDataStackLayout;
            }
        }

        private CancellationTokenSource FetchAndLoadVault()
        {
            var cts = new CancellationTokenSource();
            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                var autofillGroupings = new List<VaultListPageModel.AutofillGrouping>();
                var logins = await _loginService.GetAllAsync(Uri);

                var normalLogins = logins?.Item1.Select(l => new VaultListPageModel.AutofillLogin(l, false))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username)
                    .ToList();
                if(normalLogins?.Any() ?? false)
                {
                    autofillGroupings.Add(new VaultListPageModel.AutofillGrouping(normalLogins, AppResources.MatchingLogins));
                }

                var fuzzyLogins = logins?.Item2.Select(l => new VaultListPageModel.AutofillLogin(l, true))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username)
                    .ToList();
                if(fuzzyLogins?.Any() ?? false)
                {
                    autofillGroupings.Add(new VaultListPageModel.AutofillGrouping(fuzzyLogins,
                        AppResources.PossibleMatchingLogins));
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    if(autofillGroupings.Any())
                    {
                        PresentationLoginsGroup.ResetWithRange(autofillGroupings);
                    }

                    AdjustContent();
                });
            }, cts.Token);

            return cts;
        }

        private async void LoginSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var login = e.SelectedItem as VaultListPageModel.AutofillLogin;
            if(login == null)
            {
                return;
            }

            if(_deviceInfoService.Version < 21)
            {
                MoreClickedAsync(login);
            }
            else
            {
                bool doAutofill = true;
                if(login.Fuzzy)
                {
                    doAutofill = await UserDialogs.ConfirmAsync(
                        string.Format(AppResources.BitwardenAutofillServiceMatchConfirm, _name),
                        okText: AppResources.Yes, cancelText: AppResources.No);
                }

                if(doAutofill)
                {
                    GoogleAnalyticsService.TrackExtensionEvent("AutoFilled", Uri.StartsWith("http") ? "Website" : "App");
                    MessagingCenter.Send(Application.Current, "Autofill", login as VaultListPageModel.Login);
                }
            }

            ((ListView)sender).SelectedItem = null;
        }

        private async void AddLoginAsync()
        {
            var page = new VaultAddLoginPage(Uri, _name, true);
            await Navigation.PushForDeviceAsync(page);
        }

        private async void MoreClickedAsync(VaultListPageModel.Login login)
        {
            var buttons = new List<string> { AppResources.View, AppResources.Edit };
            if(!string.IsNullOrWhiteSpace(login.Password.Value))
            {
                buttons.Add(AppResources.CopyPassword);
            }
            if(!string.IsNullOrWhiteSpace(login.Username))
            {
                buttons.Add(AppResources.CopyUsername);
            }

            var selection = await DisplayActionSheet(login.Name, AppResources.Cancel, null, buttons.ToArray());

            if(selection == AppResources.View)
            {
                var page = new VaultViewLoginPage(login.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Edit)
            {
                var page = new VaultEditLoginPage(login.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.CopyPassword)
            {
                Copy(login.Password.Value, AppResources.Password);
            }
            else if(selection == AppResources.CopyUsername)
            {
                Copy(login.Username, AppResources.Username);
            }
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            UserDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private class AddLoginToolBarItem : ExtendedToolbarItem
        {
            public AddLoginToolBarItem(VaultAutofillListLoginsPage page)
                : base(() => page.AddLoginAsync())
            {
                Text = AppResources.Add;
                Icon = "plus";
                Priority = 2;
            }
        }

        private class SearchToolBarItem : ExtendedToolbarItem
        {
            private readonly VaultAutofillListLoginsPage _page;

            public SearchToolBarItem(VaultAutofillListLoginsPage page)
            {
                _page = page;
                Text = AppResources.Search;
                Icon = "search";
                Priority = 1;
                ClickAction = () => DoClick();
            }

            private void DoClick()
            {
                _page.GoogleAnalyticsService.TrackExtensionEvent("CloseToSearch",
                    _page.Uri.StartsWith("http") ? "Website" : "App");
                Application.Current.MainPage = new MainPage(_page.Uri);
                _page.UserDialogs.Toast(string.Format(AppResources.BitwardenAutofillServiceSearch, _page._name),
                    TimeSpan.FromSeconds(10));
            }
        }

        private class HeaderViewCell : ExtendedViewCell
        {
            public HeaderViewCell()
            {
                var label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    Style = (Style)Application.Current.Resources["text-muted"],
                    VerticalTextAlignment = TextAlignment.Center
                };

                label.SetBinding(Label.TextProperty, nameof(VaultListPageModel.AutofillGrouping.Name));

                var grid = new ContentView
                {
                    Padding = new Thickness(16, 8, 0, 8),
                    Content = label
                };

                View = grid;
                BackgroundColor = Color.FromHex("efeff4");
            }
        }
    }
}

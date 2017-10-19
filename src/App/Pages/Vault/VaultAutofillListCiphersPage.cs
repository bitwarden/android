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
    public class VaultAutofillListCiphersPage : ExtendedContentPage
    {
        private readonly ICipherService _cipherService;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly IDeviceActionService _clipboardService;
        private readonly ISettingsService _settingsService;
        private CancellationTokenSource _filterResultsCancellationTokenSource;
        private readonly string _name;

        public VaultAutofillListCiphersPage(string uriString)
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

            _cipherService = Resolver.Resolve<ICipherService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _clipboardService = Resolver.Resolve<IDeviceActionService>();
            _settingsService = Resolver.Resolve<ISettingsService>();
            UserDialogs = Resolver.Resolve<IUserDialogs>();
            GoogleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ExtendedObservableCollection<VaultListPageModel.AutofillGrouping> PresentationCiphersGroup { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.AutofillGrouping>();
        public StackLayout NoDataStackLayout { get; set; }
        public ListView ListView { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }
        private SearchToolBarItem SearchItem { get; set; }
        private AddCipherToolBarItem AddCipherItem { get; set; }
        private IGoogleAnalyticsService GoogleAnalyticsService { get; set; }
        private IUserDialogs UserDialogs { get; set; }
        private string Uri { get; set; }

        private void Init()
        {
            var noDataLabel = new Label
            {
                Text = string.Format(AppResources.NoItemsForUri, _name ?? "--"),
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            var addCipherButton = new ExtendedButton
            {
                Text = AppResources.AddAnItem,
                Command = new Command(() => AddCipherAsync()),
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            NoDataStackLayout = new StackLayout
            {
                Children = { noDataLabel, addCipherButton },
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Padding = new Thickness(20, 0),
                Spacing = 20
            };

            AddCipherItem = new AddCipherToolBarItem(this);
            ToolbarItems.Add(AddCipherItem);
            SearchItem = new SearchToolBarItem(this);
            ToolbarItems.Add(SearchItem);

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationCiphersGroup,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new HeaderViewCell()),
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(
                    (VaultListPageModel.Cipher l) => MoreClickedAsync(l)))
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ListView.RowHeight = -1;
            }

            Title = string.Format(AppResources.ItemsForUri, _name ?? "--");

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
            ListView.ItemSelected += CipherSelected;
            AddCipherItem.InitEvents();
            SearchItem.InitEvents();
            _filterResultsCancellationTokenSource = FetchAndLoadVault();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            ListView.ItemSelected -= CipherSelected;
            AddCipherItem.Dispose();
            SearchItem.Dispose();
        }

        protected override bool OnBackButtonPressed()
        {
            GoogleAnalyticsService.TrackExtensionEvent("BackClosed", Uri.StartsWith("http") ? "Website" : "App");
            MessagingCenter.Send(Application.Current, "Autofill", (VaultListPageModel.Cipher)null);
            return true;
        }

        private void AdjustContent()
        {
            if(PresentationCiphersGroup.Count > 0)
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
                var ciphers = await _cipherService.GetAllAsync(Uri);

                var normalLogins = ciphers?.Item1.Select(l => new VaultListPageModel.AutofillCipher(l, false))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Subtitle)
                    .ToList();
                if(normalLogins?.Any() ?? false)
                {
                    autofillGroupings.Add(new VaultListPageModel.AutofillGrouping(normalLogins, AppResources.MatchingItems));
                }

                var fuzzyLogins = ciphers?.Item2.Select(l => new VaultListPageModel.AutofillCipher(l, true))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username)
                    .ToList();
                if(fuzzyLogins?.Any() ?? false)
                {
                    autofillGroupings.Add(new VaultListPageModel.AutofillGrouping(fuzzyLogins,
                        AppResources.PossibleMatchingItems));
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    if(autofillGroupings.Any())
                    {
                        PresentationCiphersGroup.ResetWithRange(autofillGroupings);
                    }

                    AdjustContent();
                });
            }, cts.Token);

            return cts;
        }

        private async void CipherSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var cipher = e.SelectedItem as VaultListPageModel.AutofillCipher;
            if(cipher == null)
            {
                return;
            }

            if(_deviceInfoService.Version < 21)
            {
                MoreClickedAsync(cipher);
            }
            else
            {
                bool doAutofill = true;
                if(cipher.Fuzzy)
                {
                    doAutofill = await UserDialogs.ConfirmAsync(
                        string.Format(AppResources.BitwardenAutofillServiceMatchConfirm, _name),
                        okText: AppResources.Yes, cancelText: AppResources.No);
                }

                if(doAutofill)
                {
                    GoogleAnalyticsService.TrackExtensionEvent("AutoFilled", Uri.StartsWith("http") ? "Website" : "App");
                    MessagingCenter.Send(Application.Current, "Autofill", cipher as VaultListPageModel.Cipher);
                }
            }

            ((ListView)sender).SelectedItem = null;
        }

        private async void AddCipherAsync()
        {
            var page = new VaultAddLoginPage(Uri, _name, true);
            await Navigation.PushForDeviceAsync(page);
        }

        private async void MoreClickedAsync(VaultListPageModel.Cipher cipher)
        {
            var buttons = new List<string> { AppResources.View, AppResources.Edit };

            if(cipher.Type == Enums.CipherType.Login)
            {
                if(!string.IsNullOrWhiteSpace(cipher.Password.Value))
                {
                    buttons.Add(AppResources.CopyPassword);
                }
                if(!string.IsNullOrWhiteSpace(cipher.Username))
                {
                    buttons.Add(AppResources.CopyUsername);
                }
            }
            else if(cipher.Type == Enums.CipherType.Card)
            {
                if(!string.IsNullOrWhiteSpace(cipher.CardNumber))
                {
                    buttons.Add(AppResources.CopyNumber);
                }
                if(!string.IsNullOrWhiteSpace(cipher.CardCode.Value))
                {
                    buttons.Add(AppResources.CopySecurityCode);
                }
            }

            var selection = await DisplayActionSheet(cipher.Name, AppResources.Cancel, null, buttons.ToArray());

            if(selection == AppResources.View)
            {
                var page = new VaultViewLoginPage(cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Edit)
            {
                var page = new VaultEditLoginPage(cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.CopyPassword)
            {
                Copy(cipher.Password.Value, AppResources.Password);
            }
            else if(selection == AppResources.CopyUsername)
            {
                Copy(cipher.Username, AppResources.Username);
            }
            else if(selection == AppResources.CopyNumber)
            {
                Copy(cipher.CardNumber, AppResources.Number);
            }
            else if(selection == AppResources.CopySecurityCode)
            {
                Copy(cipher.CardCode.Value, AppResources.SecurityCode);
            }
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            UserDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private class AddCipherToolBarItem : ExtendedToolbarItem
        {
            public AddCipherToolBarItem(VaultAutofillListCiphersPage page)
                : base(() => page.AddCipherAsync())
            {
                Text = AppResources.Add;
                Icon = "plus.png";
                Priority = 2;
            }
        }

        private class SearchToolBarItem : ExtendedToolbarItem
        {
            private readonly VaultAutofillListCiphersPage _page;

            public SearchToolBarItem(VaultAutofillListCiphersPage page)
            {
                _page = page;
                Text = AppResources.Search;
                Icon = "search.png";
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

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
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using System.Collections.Generic;
using System.Threading;
using Bit.App.Enums;

namespace Bit.App.Pages
{
    public class VaultSearchCiphersPage : ExtendedContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ISyncService _syncService;
        private readonly IPushNotificationService _pushNotification;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettingsService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private CancellationTokenSource _filterResultsCancellationTokenSource;

        public VaultSearchCiphersPage()
            : base(true)
        {
            _folderService = Resolver.Resolve<IFolderService>();
            _cipherService = Resolver.Resolve<ICipherService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _syncService = Resolver.Resolve<ISyncService>();
            _pushNotification = Resolver.Resolve<IPushNotificationService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _settings = Resolver.Resolve<ISettings>();
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ExtendedObservableCollection<VaultListPageModel.NameGroup> PresentationLetters { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.NameGroup>();
        public VaultListPageModel.Cipher[] Ciphers { get; set; } = new VaultListPageModel.Cipher[] { };
        public ListView ListView { get; set; }
        public SearchBar Search { get; set; }
        public StackLayout ResultsStackLayout { get; set; }

        private void Init()
        {
            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationLetters,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new SectionHeaderViewCell(
                    nameof(VaultListPageModel.NameGroup.Name), nameof(VaultListPageModel.NameGroup.Count))),
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(
                    (VaultListPageModel.Cipher c) => MoreClickedAsync(c)))
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ListView.RowHeight = -1;
            }

            Search = new SearchBar
            {
                Placeholder = AppResources.SearchVault,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Button)),
                CancelButtonColor = Color.FromHex("3c8dbc")
            };
            // Bug with searchbar on android 7, ref https://bugzilla.xamarin.com/show_bug.cgi?id=43975
            if(Device.RuntimePlatform == Device.Android && _deviceInfoService.Version >= 24)
            {
                Search.HeightRequest = 50;
            }

            ResultsStackLayout = new StackLayout
            {
                Children = { Search, ListView },
                Spacing = 0
            };

            Title = AppResources.SearchVault;
            Content = new ActivityIndicator
            {
                IsRunning = true,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };
        }

        private void SearchBar_SearchButtonPressed(object sender, EventArgs e)
        {
            _filterResultsCancellationTokenSource = FilterResultsBackground(((SearchBar)sender).Text,
                _filterResultsCancellationTokenSource);
        }

        private void SearchBar_TextChanged(object sender, TextChangedEventArgs e)
        {
            var oldLength = e.OldTextValue?.Length ?? 0;
            var newLength = e.NewTextValue?.Length ?? 0;
            if(oldLength < 2 && newLength < 2 && oldLength < newLength)
            {
                return;
            }

            _filterResultsCancellationTokenSource = FilterResultsBackground(e.NewTextValue,
                _filterResultsCancellationTokenSource);
        }

        private CancellationTokenSource FilterResultsBackground(string searchFilter,
            CancellationTokenSource previousCts)
        {
            var cts = new CancellationTokenSource();
            Task.Run(async () =>
            {
                if(!string.IsNullOrWhiteSpace(searchFilter))
                {
                    await Task.Delay(300);
                    if(searchFilter != Search.Text)
                    {
                        return;
                    }
                    else
                    {
                        previousCts?.Cancel();
                    }
                }

                try
                {
                    FilterResults(searchFilter, cts.Token);
                }
                catch(OperationCanceledException) { }
            }, cts.Token);

            return cts;
        }

        private void FilterResults(string searchFilter, CancellationToken ct)
        {
            ct.ThrowIfCancellationRequested();

            if(string.IsNullOrWhiteSpace(searchFilter))
            {
                LoadLetters(Ciphers, ct);
            }
            else
            {
                searchFilter = searchFilter.ToLower();
                var filteredCiphers = Ciphers
                    .Where(s => s.Name.ToLower().Contains(searchFilter) ||
                        (s.Subtitle?.ToLower().Contains(searchFilter) ?? false))
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToArray();

                ct.ThrowIfCancellationRequested();
                LoadLetters(filteredCiphers, ct);
            }
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            MessagingCenter.Subscribe<ISyncService, bool>(_syncService, "SyncCompleted", (sender, success) =>
            {
                if(success)
                {
                    _filterResultsCancellationTokenSource = FetchAndLoadVault();
                }
            });

            ListView.ItemSelected += CipherSelected;
            Search.TextChanged += SearchBar_TextChanged;
            Search.SearchButtonPressed += SearchBar_SearchButtonPressed;

            _filterResultsCancellationTokenSource = FetchAndLoadVault();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            MessagingCenter.Unsubscribe<ISyncService, bool>(_syncService, "SyncCompleted");

            ListView.ItemSelected -= CipherSelected;
            Search.TextChanged -= SearchBar_TextChanged;
            Search.SearchButtonPressed -= SearchBar_SearchButtonPressed;
        }

        private CancellationTokenSource FetchAndLoadVault()
        {
            var cts = new CancellationTokenSource();
            if(PresentationLetters.Count > 0 && _syncService.SyncInProgress)
            {
                return cts;
            }

            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                var ciphers = await _cipherService.GetAllAsync();

                Ciphers = ciphers
                    .Select(s => new VaultListPageModel.Cipher(s, _appSettingsService))
                    .OrderBy(s =>
                    {
                        // Sort numbers and letters before special characters
                        return !string.IsNullOrWhiteSpace(s.Name) && s.Name.Length > 0 &&
                            Char.IsLetterOrDigit(s.Name[0]) ? 0 : 1;
                    })
                    .ThenBy(s => s.Name)
                    .ThenBy(s => s.Subtitle)
                    .ToArray();

                try
                {
                    FilterResults(Search.Text, cts.Token);
                }
                catch(OperationCanceledException) { }
            }, cts.Token);

            return cts;
        }

        private void LoadLetters(VaultListPageModel.Cipher[] ciphers, CancellationToken ct)
        {
            ct.ThrowIfCancellationRequested();
            var letterGroups = ciphers.GroupBy(c => c.NameGroup)
                .Select(g => new VaultListPageModel.NameGroup(g.Key, g.ToList()));
            ct.ThrowIfCancellationRequested();
            Device.BeginInvokeOnMainThread(() =>
            {
                PresentationLetters.ResetWithRange(letterGroups);
                Content = ResultsStackLayout;
            });
        }

        private async void CipherSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var cipher = e.SelectedItem as VaultListPageModel.Cipher;
            if(cipher == null)
            {
                return;
            }

            var page = new VaultViewCipherPage(cipher.Type, cipher.Id);
            await Navigation.PushForDeviceAsync(page);
            ((ListView)sender).SelectedItem = null;
        }

        private async void MoreClickedAsync(VaultListPageModel.Cipher cipher)
        {
            var buttons = new List<string> { AppResources.View, AppResources.Edit };

            if(cipher.Type == CipherType.Login)
            {
                if(!string.IsNullOrWhiteSpace(cipher.LoginPassword.Value))
                {
                    buttons.Add(AppResources.CopyPassword);
                }
                if(!string.IsNullOrWhiteSpace(cipher.LoginUsername))
                {
                    buttons.Add(AppResources.CopyUsername);
                }
                if(!string.IsNullOrWhiteSpace(cipher.LoginUri) && (cipher.LoginUri.StartsWith("http://")
                    || cipher.LoginUri.StartsWith("https://")))
                {
                    buttons.Add(AppResources.GoToWebsite);
                }
            }
            else if(cipher.Type == CipherType.Card)
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
                var page = new VaultViewCipherPage(cipher.Type, cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Edit)
            {
                var page = new VaultEditCipherPage(cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.CopyPassword)
            {
                Copy(cipher.LoginPassword.Value, AppResources.Password);
            }
            else if(selection == AppResources.CopyUsername)
            {
                Copy(cipher.LoginUsername, AppResources.Username);
            }
            else if(selection == AppResources.GoToWebsite)
            {
                Device.OpenUri(new Uri(cipher.LoginUri));
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
            _deviceActionService.CopyToClipboard(copyText);
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }
    }
}

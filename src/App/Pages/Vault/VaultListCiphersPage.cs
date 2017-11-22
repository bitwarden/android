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
using FFImageLoading.Forms;
using Bit.App.Enums;

namespace Bit.App.Pages
{
    public class VaultListCiphersPage : ExtendedContentPage
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
        private readonly bool _favorites;
        private CancellationTokenSource _filterResultsCancellationTokenSource;

        public VaultListCiphersPage(bool favorites, string uri = null)
            : base(true)
        {
            _favorites = favorites;
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

            var cryptoService = Resolver.Resolve<ICryptoService>();

            Uri = uri;

            Init();
        }

        public ExtendedObservableCollection<VaultListPageModel.Folder> PresentationFolders { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.Folder>();
        public ListView ListView { get; set; }
        public VaultListPageModel.Cipher[] Ciphers { get; set; } = new VaultListPageModel.Cipher[] { };
        public VaultListPageModel.Folder[] Folders { get; set; } = new VaultListPageModel.Folder[] { };
        public SearchBar Search { get; set; }
        public StackLayout NoDataStackLayout { get; set; }
        public StackLayout ResultsStackLayout { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }
        private AddCipherToolBarItem AddCipherItem { get; set; }
        public string Uri { get; set; }

        private void Init()
        {
            if(!_favorites)
            {
                AddCipherItem = new AddCipherToolBarItem(this);
                ToolbarItems.Add(AddCipherItem);
            }

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationFolders,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new VaultListHeaderViewCell(this)),
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

            Title = _favorites ? AppResources.Favorites : AppResources.MyVault;

            ResultsStackLayout = new StackLayout
            {
                Children = { Search, ListView },
                Spacing = 0
            };

            var noDataLabel = new Label
            {
                Text = _favorites ? AppResources.NoFavorites : AppResources.NoItems,
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            NoDataStackLayout = new StackLayout
            {
                Children = { noDataLabel },
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Padding = new Thickness(20, 0),
                Spacing = 20
            };

            if(!_favorites)
            {
                var addCipherButton = new ExtendedButton
                {
                    Text = AppResources.AddAnItem,
                    Command = new Command(() => AddCipher()),
                    Style = (Style)Application.Current.Resources["btn-primaryAccent"]
                };

                NoDataStackLayout.Children.Add(addCipherButton);
            }

            LoadingIndicator = new ActivityIndicator
            {
                IsRunning = true,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            Content = LoadingIndicator;
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
                LoadFolders(Ciphers, ct);
            }
            else
            {
                searchFilter = searchFilter.ToLower();
                var filteredCiphers = Ciphers
                    .Where(s => 
                        s.Name.ToLower().Contains(searchFilter) || 
                        (s.Subtitle?.ToLower().Contains(searchFilter) ?? false))
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToArray();

                ct.ThrowIfCancellationRequested();
                LoadFolders(filteredCiphers, ct);
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
            AddCipherItem?.InitEvents();

            _filterResultsCancellationTokenSource = FetchAndLoadVault();

            if(_connectivity.IsConnected && Device.RuntimePlatform == Device.iOS && !_favorites)
            {
                var pushPromptShow = _settings.GetValueOrDefault(Constants.PushInitialPromptShown, false);
                Action registerAction = () =>
                {
                    var lastPushRegistration = 
                        _settings.GetValueOrDefault(Constants.PushLastRegistrationDate, DateTime.MinValue);
                    if(!pushPromptShow || DateTime.UtcNow - lastPushRegistration > TimeSpan.FromDays(1))
                    {
                        _pushNotification.Register();
                    }
                };

                if(!pushPromptShow)
                {
                    _settings.AddOrUpdateValue(Constants.PushInitialPromptShown, true);
                    _userDialogs.Alert(new AlertConfig
                    {
                        Message = AppResources.PushNotificationAlert,
                        Title = AppResources.EnableAutomaticSyncing,
                        OnAction = registerAction,
                        OkText = AppResources.OkGotIt
                    });
                }
                else
                {
                    // Check push registration once per day
                    registerAction();
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            MessagingCenter.Unsubscribe<ISyncService, bool>(_syncService, "SyncCompleted");

            ListView.ItemSelected -= CipherSelected;
            Search.TextChanged -= SearchBar_TextChanged;
            Search.SearchButtonPressed -= SearchBar_SearchButtonPressed;
            AddCipherItem?.Dispose();
        }

        protected override bool OnBackButtonPressed()
        {
            if(string.IsNullOrWhiteSpace(Uri))
            {
                return false;
            }

            _googleAnalyticsService.TrackExtensionEvent("BackClosed", Uri.StartsWith("http") ? "Website" : "App");
            _deviceActionService.CloseAutofill();
            return true;
        }

        private void AdjustContent()
        {
            if(PresentationFolders.Count > 0 || !string.IsNullOrWhiteSpace(Search.Text))
            {
                Content = ResultsStackLayout;
            }
            else
            {
                Content = NoDataStackLayout;
            }
        }

        private CancellationTokenSource FetchAndLoadVault()
        {
            var cts = new CancellationTokenSource();
            if(PresentationFolders.Count > 0 && _syncService.SyncInProgress)
            {
                return cts;
            }

            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                var foldersTask = _folderService.GetAllAsync();
                var ciphersTask = _favorites ? _cipherService.GetAllAsync(true) : _cipherService.GetAllAsync();
                await Task.WhenAll(foldersTask, ciphersTask);

                var folders = await foldersTask;
                var ciphers = await ciphersTask;

                Folders = folders
                    .Select(f => new VaultListPageModel.Folder(f))
                    .OrderBy(s => s.Name)
                    .ToArray();

                Ciphers = ciphers
                    .Select(s => new VaultListPageModel.Cipher(s, _appSettingsService))
                    .OrderBy(s => s.Name)
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

        private void LoadFolders(VaultListPageModel.Cipher[] ciphers, CancellationToken ct)
        {
            var folders = new List<VaultListPageModel.Folder>(Folders);

            foreach(var folder in folders)
            {
                if(folder.Any())
                {
                    folder.Clear();
                }

                var ciphersToAdd = ciphers
                    .Where(s => s.FolderId == folder.Id)
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToList();

                ct.ThrowIfCancellationRequested();
                folder.AddRange(ciphersToAdd);
            }

            var noneToAdd = ciphers
                .Where(s => s.FolderId == null)
                .TakeWhile(s => !ct.IsCancellationRequested)
                .ToList();

            ct.ThrowIfCancellationRequested();

            var noneFolder = new VaultListPageModel.Folder(noneToAdd);
            folders.Add(noneFolder);

            var foldersToAdd = folders
                .Where(f => f.Any())
                .TakeWhile(s => !ct.IsCancellationRequested)
                .ToList();

            ct.ThrowIfCancellationRequested();
            Device.BeginInvokeOnMainThread(() =>
            {
                PresentationFolders.ResetWithRange(foldersToAdd);
                AdjustContent();
            });
        }

        private async void CipherSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var cipher = e.SelectedItem as VaultListPageModel.Cipher;
            if(cipher == null)
            {
                return;
            }

            string selection = null;
            if(!string.IsNullOrWhiteSpace(Uri))
            {
                selection = await DisplayActionSheet(AppResources.AutofillOrView, AppResources.Cancel, null,
                    AppResources.Autofill, AppResources.View);
            }

            if(selection == AppResources.View || string.IsNullOrWhiteSpace(Uri))
            {
                var page = new VaultViewCipherPage(cipher.Type, cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Autofill)
            {
                if(_deviceInfoService.Version < 21)
                {
                    MoreClickedAsync(cipher);
                }
                else
                {
                    _googleAnalyticsService.TrackExtensionEvent("AutoFilled", 
                        Uri.StartsWith("http") ? "Website" : "App");
                    _deviceActionService.Autofill(cipher);
                }
            }

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

        private async void AddCipher()
        {
            var type = await _userDialogs.ActionSheetAsync(AppResources.SelectTypeAdd, AppResources.Cancel, null, null,
                AppResources.TypeLogin, AppResources.TypeCard, AppResources.TypeIdentity, AppResources.TypeSecureNote);

            var selectedType = CipherType.SecureNote;
            if(type == AppResources.Cancel)
            {
                return;
            }
            else if(type == AppResources.TypeLogin)
            {
                selectedType = CipherType.Login;
            }
            else if(type == AppResources.TypeCard)
            {
                selectedType = CipherType.Card;
            }
            else if(type == AppResources.TypeIdentity)
            {
                selectedType = CipherType.Identity;
            }

            var page = new VaultAddCipherPage(selectedType, Uri);
            await Navigation.PushForDeviceAsync(page);
        }

        private class AddCipherToolBarItem : ExtendedToolbarItem
        {
            private readonly VaultListCiphersPage _page;

            public AddCipherToolBarItem(VaultListCiphersPage page)
                : base(() => page.AddCipher())
            {
                _page = page;
                Text = AppResources.Add;
                Icon = "plus.png";
            }
        }

        private class VaultListHeaderViewCell : ExtendedViewCell
        {
            public VaultListHeaderViewCell(VaultListCiphersPage page)
            {
                var image = new CachedImage
                {
                    Source = "folder.png",
                    WidthRequest = 20,
                    HeightRequest = 20,
                    HorizontalOptions = LayoutOptions.Center,
                    VerticalOptions = LayoutOptions.Center
                };

                var label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    Style = (Style)Application.Current.Resources["text-muted"],
                    VerticalTextAlignment = TextAlignment.Center
                };

                label.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Folder.Name));

                var grid = new Grid
                {
                    ColumnSpacing = 0,
                    RowSpacing = 0,
                    Padding = new Thickness(3, 8, 0, 8)
                };
                grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(40, GridUnitType.Absolute) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                grid.Children.Add(image, 0, 0);
                grid.Children.Add(label, 1, 0);

                View = grid;
                BackgroundColor = Color.FromHex("efeff4");
            }
        }
    }
}

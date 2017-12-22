using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Utilities;
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using System.Threading;
using static Bit.App.Models.Page.VaultListPageModel;
using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class VaultListCiphersPage : ExtendedContentPage
    {
        private readonly ICipherService _cipherService;
        private readonly IConnectivity _connectivity;
        private readonly ISyncService _syncService;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettingsService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly IDeviceActionService _deviceActionService;
        private CancellationTokenSource _filterResultsCancellationTokenSource;
        private readonly bool _favorites = false;
        private readonly bool _folder = false;
        private readonly string _folderId = null;
        private readonly string _collectionId = null;
        private readonly string _groupingName = null;
        private readonly string _uri = null;

        public VaultListCiphersPage(bool folder = false, string folderId = null,
            string collectionId = null, string groupingName = null, bool favorites = false, string uri = null)
            : base(true)
        {
            _folder = folder;
            _folderId = folderId;
            _collectionId = collectionId;
            _favorites = favorites;
            _groupingName = groupingName;
            _uri = uri;

            _cipherService = Resolver.Resolve<ICipherService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _syncService = Resolver.Resolve<ISyncService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _settings = Resolver.Resolve<ISettings>();
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();

            Init();
        }

        public ExtendedObservableCollection<Section<Cipher>> PresentationSections { get; private set; }
            = new ExtendedObservableCollection<Section<Cipher>>();
        public Cipher[] Ciphers { get; set; } = new Cipher[] { };
        public ListView ListView { get; set; }
        public SearchBar Search { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }
        public StackLayout NoDataStackLayout { get; set; }
        public StackLayout ResultsStackLayout { get; set; }
        private AddCipherToolBarItem AddCipherItem { get; set; }

        private void Init()
        {
            if(!string.IsNullOrWhiteSpace(_uri) || _folder || !string.IsNullOrWhiteSpace(_folderId))
            {
                AddCipherItem = new AddCipherToolBarItem(this, _folderId);
                ToolbarItems.Add(AddCipherItem);
            }

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationSections,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new SectionHeaderViewCell(nameof(Section<Cipher>.Name),
                    nameof(Section<Cipher>.Count), new Thickness(16, 8, 27, 8))),
                GroupShortNameBinding = new Binding(nameof(Section<Cipher>.Name)),
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(
                    (Cipher c) => Helpers.CipherMoreClickedAsync(this, c, !string.IsNullOrWhiteSpace(_uri))))
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ListView.RowHeight = -1;
            }

            Search = new SearchBar
            {
                Placeholder = AppResources.Search,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Button)),
                CancelButtonColor = Color.FromHex("3c8dbc")
            };
            // Bug with search bar on android 7, ref https://bugzilla.xamarin.com/show_bug.cgi?id=43975
            if(Device.RuntimePlatform == Device.Android && _deviceInfoService.Version >= 24)
            {
                Search.HeightRequest = 50;
            }

            var noDataLabel = new Label
            {
                Text = _favorites ? AppResources.NoFavorites : AppResources.NoItems,
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            if(_folder || !string.IsNullOrWhiteSpace(_folderId))
            {
                noDataLabel.Text = AppResources.NoItemsFolder;
            }
            else if(!string.IsNullOrWhiteSpace(_collectionId))
            {
                noDataLabel.Text = AppResources.NoItemsCollection;
            }

            NoDataStackLayout = new StackLayout
            {
                Children = { noDataLabel },
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Padding = new Thickness(20, 0),
                Spacing = 20
            };

            if(string.IsNullOrWhiteSpace(_collectionId) && !_favorites)
            {
                NoDataStackLayout.Children.Add(new ExtendedButton
                {
                    Text = AppResources.AddAnItem,
                    Command = new Command(() => Helpers.AddCipher(this, _folderId)),
                    Style = (Style)Application.Current.Resources["btn-primaryAccent"]
                });
            }

            ResultsStackLayout = new StackLayout
            {
                Children = { Search, ListView },
                Spacing = 0
            };

            if(!string.IsNullOrWhiteSpace(_groupingName))
            {
                Title = _groupingName;
            }
            else if(_favorites)
            {
                Title = AppResources.Favorites;
            }
            else
            {
                Title = AppResources.SearchVault;

                if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.UWP)
                {
                    ToolbarItems.Add(new DismissModalToolBarItem(this));
                }
            }

            LoadingIndicator = new ActivityIndicator
            {
                IsRunning = true
            };

            if(Device.RuntimePlatform != Device.UWP)
            {
                LoadingIndicator.VerticalOptions = LayoutOptions.CenterAndExpand;
                LoadingIndicator.HorizontalOptions = LayoutOptions.Center;
            }

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
                LoadSections(Ciphers, ct);
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
                LoadSections(filteredCiphers, ct);
            }
        }

        protected override bool OnBackButtonPressed()
        {
            if(string.IsNullOrWhiteSpace(_uri))
            {
                return false;
            }

            _googleAnalyticsService.TrackExtensionEvent("BackClosed", _uri.StartsWith("http") ? "Website" : "App");
            _deviceActionService.CloseAutofill();
            return true;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
            {
                if(success)
                {
                    _filterResultsCancellationTokenSource = FetchAndLoadVault();
                }
            });

            AddCipherItem?.InitEvents();
            ListView.ItemSelected += CipherSelected;
            Search.TextChanged += SearchBar_TextChanged;
            Search.SearchButtonPressed += SearchBar_SearchButtonPressed;
            _filterResultsCancellationTokenSource = FetchAndLoadVault();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            MessagingCenter.Unsubscribe<Application, bool>(Application.Current, "SyncCompleted");

            AddCipherItem?.Dispose();
            ListView.ItemSelected -= CipherSelected;
            Search.TextChanged -= SearchBar_TextChanged;
            Search.SearchButtonPressed -= SearchBar_SearchButtonPressed;
        }

        private CancellationTokenSource FetchAndLoadVault()
        {
            var cts = new CancellationTokenSource();
            if(PresentationSections.Count > 0 && _syncService.SyncInProgress)
            {
                return cts;
            }

            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                IEnumerable<Models.Cipher> ciphers;
                if(_folder || !string.IsNullOrWhiteSpace(_folderId))
                {
                    ciphers = await _cipherService.GetAllByFolderAsync(_folderId);
                }
                else if(!string.IsNullOrWhiteSpace(_collectionId))
                {
                    ciphers = await _cipherService.GetAllByCollectionAsync(_collectionId);
                }
                else if(_favorites)
                {
                    ciphers = await _cipherService.GetAllAsync(true);
                }
                else
                {
                    ciphers = await _cipherService.GetAllAsync();
                }

                Ciphers = ciphers
                    .Select(s => new Cipher(s, _appSettingsService))
                    .OrderBy(s =>
                    {
                        // Sort numbers and letters before special characters
                        return !string.IsNullOrWhiteSpace(s.Name) && s.Name.Length > 0 &&
                           Char.IsDigit(s.Name[0]) ? 0 : Char.IsLetter(s.Name[0]) ? 1 : 2;
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

        private void LoadSections(Cipher[] ciphers, CancellationToken ct)
        {
            ct.ThrowIfCancellationRequested();
            var sections = ciphers.GroupBy(c => c.NameGroup.ToUpperInvariant())
                .Select(g => new Section<Cipher>(g.ToList(), g.Key));
            ct.ThrowIfCancellationRequested();
            Device.BeginInvokeOnMainThread(() =>
            {
                PresentationSections.ResetWithRange(sections);
                if(PresentationSections.Count > 0 || !string.IsNullOrWhiteSpace(Search.Text))
                {
                    Content = ResultsStackLayout;

                    if(string.IsNullOrWhiteSpace(_uri) && !_folder && string.IsNullOrWhiteSpace(_folderId) &&
                        string.IsNullOrWhiteSpace(_collectionId) && !_favorites)
                    {
                        Search.Focus();
                    }
                }
                else if(_syncService.SyncInProgress)
                {
                    Content = LoadingIndicator;
                }
                else
                {
                    Content = NoDataStackLayout;
                }
            });
        }

        private async void CipherSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var cipher = e.SelectedItem as Cipher;
            if(cipher == null)
            {
                return;
            }

            string selection = null;
            if(!string.IsNullOrWhiteSpace(_uri))
            {
                selection = await DisplayActionSheet(AppResources.AutofillOrView, AppResources.Cancel, null,
                    AppResources.Autofill, AppResources.View);
            }

            if(selection == AppResources.View || string.IsNullOrWhiteSpace(_uri))
            {
                var page = new VaultViewCipherPage(cipher.Type, cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Autofill)
            {
                if(_deviceInfoService.Version < 21)
                {
                    Helpers.CipherMoreClickedAsync(this, cipher, !string.IsNullOrWhiteSpace(_uri));
                }
                else
                {
                    _googleAnalyticsService.TrackExtensionEvent("AutoFilled",
                        _uri.StartsWith("http") ? "Website" : "App");
                    _deviceActionService.Autofill(cipher);
                }
            }

            ((ListView)sender).SelectedItem = null;
        }
    }
}

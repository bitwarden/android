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
using PushNotification.Plugin.Abstractions;
using Plugin.Settings.Abstractions;
using Plugin.Connectivity.Abstractions;
using System.Collections.Generic;
using System.Threading;

namespace Bit.App.Pages
{
    public class VaultListSitesPage : ExtendedContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ISiteService _siteService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IClipboardService _clipboardService;
        private readonly ISyncService _syncService;
        private readonly IPushNotification _pushNotification;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly ISettings _settings;
        private readonly bool _favorites;
        private bool _loadExistingData;
        private CancellationTokenSource _filterResultsCancellationTokenSource;

        public VaultListSitesPage(bool favorites)
            : base(true)
        {
            _favorites = favorites;
            _folderService = Resolver.Resolve<IFolderService>();
            _siteService = Resolver.Resolve<ISiteService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();
            _syncService = Resolver.Resolve<ISyncService>();
            _pushNotification = Resolver.Resolve<IPushNotification>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _settings = Resolver.Resolve<ISettings>();

            var cryptoService = Resolver.Resolve<ICryptoService>();
            _loadExistingData = !_settings.GetValueOrDefault(Constants.FirstVaultLoad, true) || !cryptoService.KeyChanged;

            Init();
        }

        public ExtendedObservableCollection<VaultListPageModel.Folder> PresentationFolders { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.Folder>();
        public ListView ListView { get; set; }
        public VaultListPageModel.Site[] Sites { get; set; } = new VaultListPageModel.Site[] { };
        public VaultListPageModel.Folder[] Folders { get; set; } = new VaultListPageModel.Folder[] { };
        public SearchBar Search { get; set; }
        public StackLayout NoDataStackLayout { get; set; }
        public StackLayout ResultsStackLayout { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }

        private void Init()
        {
            MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
            {
                if(success)
                {
                    _filterResultsCancellationTokenSource = FetchAndLoadVault();
                }
            });

            if(!_favorites)
            {
                ToolbarItems.Add(new AddSiteToolBarItem(this));
            }

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationFolders,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new VaultListHeaderViewCell(this)),
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(this))
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                ListView.RowHeight = -1;
            }

            ListView.ItemSelected += SiteSelected;

            Search = new SearchBar
            {
                Placeholder = AppResources.SearchVault,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Button)),
                CancelButtonColor = Color.FromHex("3c8dbc")
            };
            Search.TextChanged += SearchBar_TextChanged;
            Search.SearchButtonPressed += SearchBar_SearchButtonPressed;
            // Bug with searchbar on android 7, ref https://bugzilla.xamarin.com/show_bug.cgi?id=43975
            if(Device.OS == TargetPlatform.Android && _deviceInfoService.Version >= 24)
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
                Text = _favorites ? AppResources.NoFavorites : AppResources.NoSites,
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
                var addSiteButton = new ExtendedButton
                {
                    Text = AppResources.AddASite,
                    Command = new Command(() => AddSite()),
                    Style = (Style)Application.Current.Resources["btn-primaryAccent"]
                };

                NoDataStackLayout.Children.Add(addSiteButton);
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

        private CancellationTokenSource FilterResultsBackground(string searchFilter, CancellationTokenSource previousCts)
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
                LoadFolders(Sites, ct);
            }
            else
            {
                searchFilter = searchFilter.ToLower();
                var filteredSites = Sites
                    .Where(s => s.Name.ToLower().Contains(searchFilter) || s.Username.ToLower().Contains(searchFilter))
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToArray();

                ct.ThrowIfCancellationRequested();
                LoadFolders(filteredSites, ct);
            }
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if(_loadExistingData)
            {
                _filterResultsCancellationTokenSource = FetchAndLoadVault();
            }

            if(_connectivity.IsConnected && Device.OS == TargetPlatform.iOS && !_favorites)
            {
                var pushPromptShow = _settings.GetValueOrDefault(Constants.PushInitialPromptShown, false);
                Action registerAction = () =>
                {
                    var lastPushRegistration = _settings.GetValueOrDefault<DateTime?>(Constants.PushLastRegistrationDate, null);
                    if(!pushPromptShow || !lastPushRegistration.HasValue
                        || (DateTime.UtcNow - lastPushRegistration) > TimeSpan.FromDays(1))
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
            _settings.AddOrUpdateValue(Constants.FirstVaultLoad, false);
            _loadExistingData = true;

            if(PresentationFolders.Count > 0 && _syncService.SyncInProgress)
            {
                return cts;
            }

            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                var foldersTask = _folderService.GetAllAsync();
                var sitesTask = _favorites ? _siteService.GetAllAsync(true) : _siteService.GetAllAsync();
                await Task.WhenAll(foldersTask, sitesTask);

                var folders = await foldersTask;
                var sites = await sitesTask;

                Folders = folders
                    .Select(f => new VaultListPageModel.Folder(f))
                    .OrderBy(s => s.Name)
                    .ToArray();

                Sites = sites
                    .Select(s => new VaultListPageModel.Site(s))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username)
                    .ToArray();

                try
                {
                    FilterResults(Search.Text, cts.Token);
                }
                catch(OperationCanceledException) { }
            }, cts.Token);

            return cts;
        }

        private void LoadFolders(VaultListPageModel.Site[] sites, CancellationToken ct)
        {
            var folders = new List<VaultListPageModel.Folder>(Folders);

            foreach(var folder in folders)
            {
                if(folder.Any())
                {
                    folder.Clear();
                }

                var sitesToAdd = sites
                    .Where(s => s.FolderId == folder.Id)
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToList();

                ct.ThrowIfCancellationRequested();
                folder.AddRange(sitesToAdd);
            }

            var noneToAdd = sites
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

        private async void SiteSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var site = e.SelectedItem as VaultListPageModel.Site;
            var page = new VaultViewSitePage(site.Id);
            await Navigation.PushForDeviceAsync(page);
        }

        private async void MoreClickedAsync(VaultListPageModel.Site site)
        {
            var buttons = new List<string> { AppResources.View, AppResources.Edit };
            if(!string.IsNullOrWhiteSpace(site.Password.Value))
            {
                buttons.Add(AppResources.CopyPassword);
            }
            if(!string.IsNullOrWhiteSpace(site.Username))
            {
                buttons.Add(AppResources.CopyUsername);
            }
            if(!string.IsNullOrWhiteSpace(site.Uri.Value) && (site.Uri.Value.StartsWith("http://")
                || site.Uri.Value.StartsWith("https://")))
            {
                buttons.Add(AppResources.GoToWebsite);
            }

            var selection = await DisplayActionSheet(site.Name, AppResources.Cancel, null, buttons.ToArray());

            if(selection == AppResources.View)
            {
                var page = new VaultViewSitePage(site.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Edit)
            {
                var page = new VaultEditSitePage(site.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.CopyPassword)
            {
                Copy(site.Password.Value, AppResources.Password);
            }
            else if(selection == AppResources.CopyUsername)
            {
                Copy(site.Username, AppResources.Username);
            }
            else if(selection == AppResources.GoToWebsite)
            {
                Device.OpenUri(new Uri(site.Uri.Value));
            }
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private async void AddSite()
        {
            var page = new VaultAddSitePage();
            await Navigation.PushForDeviceAsync(page);
        }

        private class AddSiteToolBarItem : ToolbarItem
        {
            private readonly VaultListSitesPage _page;

            public AddSiteToolBarItem(VaultListSitesPage page)
            {
                _page = page;
                Text = AppResources.Add;
                Icon = "plus";
                Clicked += ClickedItem;
            }

            private void ClickedItem(object sender, EventArgs e)
            {
                _page.AddSite();
            }
        }

        private class VaultListViewCell : LabeledDetailCell
        {
            private VaultListSitesPage _page;

            public static readonly BindableProperty SiteParameterProperty = BindableProperty.Create(nameof(SiteParameter),
                typeof(VaultListPageModel.Site), typeof(VaultListViewCell), null);

            public VaultListViewCell(VaultListSitesPage page)
            {
                _page = page;

                SetBinding(SiteParameterProperty, new Binding("."));
                Label.SetBinding<VaultListPageModel.Site>(Label.TextProperty, s => s.Name);
                Detail.SetBinding<VaultListPageModel.Site>(Label.TextProperty, s => s.Username);

                Button.Image = "more";
                Button.Command = new Command(() => ShowMore());
                Button.BackgroundColor = Color.Transparent;

                BackgroundColor = Color.White;
            }

            public VaultListPageModel.Site SiteParameter
            {
                get { return GetValue(SiteParameterProperty) as VaultListPageModel.Site; }
                set { SetValue(SiteParameterProperty, value); }
            }

            private void ShowMore()
            {
                _page.MoreClickedAsync(SiteParameter);
            }
        }

        private class VaultListHeaderViewCell : ExtendedViewCell
        {
            public VaultListHeaderViewCell(VaultListSitesPage page)
            {
                var image = new Image
                {
                    Source = "folder",
                    WidthRequest = 18,
                    HeightRequest = 18
                };

                var label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    Style = (Style)Application.Current.Resources["text-muted"]
                };

                label.SetBinding<VaultListPageModel.Folder>(Label.TextProperty, s => s.Name);

                var grid = new Grid
                {
                    ColumnSpacing = 10,
                    Padding = new Thickness(16, 8, 0, 8)
                };
                grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(18, GridUnitType.Absolute) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                grid.Children.Add(image, 0, 0);
                grid.Children.Add(label, 1, 0);

                View = grid;
                BackgroundColor = Color.FromHex("efeff4");
            }
        }
    }
}

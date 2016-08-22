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
                Placeholder = "Search vault",
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Button)),
                CancelButtonColor = Color.FromHex("3c8dbc")
            };
            Search.TextChanged += SearchBar_TextChanged;
            Search.SearchButtonPressed += SearchBar_SearchButtonPressed;

            Title = _favorites ? AppResources.Favorites : AppResources.MyVault;
            Content = new StackLayout
            {
                Children = { Search, ListView },
                Spacing = 0
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
                var pushPromptShow = _settings.GetValueOrDefault<bool>(Constants.PushInitialPromptShown);
                Action registerAction = () =>
                {
                    var lastPushRegistration = _settings.GetValueOrDefault<DateTime?>(Constants.PushLastRegistrationDate);
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
                        Message = "bitwarden keeps your vault automatically synced by using push notifications."
                        + " For the best possible experience, please select \"Ok\" on the following prompt when"
                        + " asked to enable push notifications.",
                        Title = "Enable Automatic Syncing",
                        OnOk = registerAction,
                        OkText = "Ok, got it!"
                    });
                }
                else
                {
                    // Check push registration once per day
                    registerAction();
                }
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
            Device.BeginInvokeOnMainThread(() => PresentationFolders.ResetWithRange(foldersToAdd));
        }

        private void SiteSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var site = e.SelectedItem as VaultListPageModel.Site;
            var page = new ExtendedNavigationPage(new VaultViewSitePage(site.Id));
            Navigation.PushModalAsync(page);
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
                var page = new ExtendedNavigationPage(new VaultViewSitePage(site.Id));
                await Navigation.PushModalAsync(page);
            }
            else if(selection == AppResources.Edit)
            {
                var page = new ExtendedNavigationPage(new VaultEditSitePage(site.Id));
                await Navigation.PushModalAsync(page);
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

            private async void ClickedItem(object sender, EventArgs e)
            {
                var page = new ExtendedNavigationPage(new VaultAddSitePage());
                await _page.Navigation.PushModalAsync(page);
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
                    VerticalOptions = LayoutOptions.CenterAndExpand
                };

                var label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    VerticalTextAlignment = TextAlignment.Center,
                    VerticalOptions = LayoutOptions.CenterAndExpand,
                    Style = (Style)Application.Current.Resources["text-muted"]
                };

                label.SetBinding<VaultListPageModel.Folder>(Label.TextProperty, s => s.Name);

                var stackLayout = new StackLayout
                {
                    Orientation = StackOrientation.Horizontal,
                    VerticalOptions = LayoutOptions.FillAndExpand,
                    Children = { image, label },
                    Padding = new Thickness(16, 0, 0, 0)
                };

                View = stackLayout;
                Height = 40;
                BackgroundColor = Color.FromHex("efeff4");
            }
        }
    }
}

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
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class VaultListLoginsPage : ExtendedContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ILoginService _loginService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IDeviceActionService _clipboardService;
        private readonly ISyncService _syncService;
        private readonly IPushNotification _pushNotification;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly ISettings _settings;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly bool _favorites;
        private CancellationTokenSource _filterResultsCancellationTokenSource;

        public VaultListLoginsPage(bool favorites, string uri = null)
            : base(true)
        {
            _favorites = favorites;
            _folderService = Resolver.Resolve<IFolderService>();
            _loginService = Resolver.Resolve<ILoginService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IDeviceActionService>();
            _syncService = Resolver.Resolve<ISyncService>();
            _pushNotification = Resolver.Resolve<IPushNotification>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _settings = Resolver.Resolve<ISettings>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            var cryptoService = Resolver.Resolve<ICryptoService>();

            Uri = uri;

            Init();
        }

        public ExtendedObservableCollection<VaultListPageModel.Folder> PresentationFolders { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.Folder>();
        public ListView ListView { get; set; }
        public VaultListPageModel.Login[] Logins { get; set; } = new VaultListPageModel.Login[] { };
        public VaultListPageModel.Folder[] Folders { get; set; } = new VaultListPageModel.Folder[] { };
        public SearchBar Search { get; set; }
        public StackLayout NoDataStackLayout { get; set; }
        public StackLayout ResultsStackLayout { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }
        private AddLoginToolBarItem AddLoginItem { get; set; }
        public string Uri { get; set; }

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
                AddLoginItem = new AddLoginToolBarItem(this);
                ToolbarItems.Add(AddLoginItem);
            }

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationFolders,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new VaultListHeaderViewCell(this)),
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(
                    (VaultListPageModel.Login l) => MoreClickedAsync(l)))
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
                Text = _favorites ? AppResources.NoFavorites : AppResources.NoLogins,
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
                var addLoginButton = new ExtendedButton
                {
                    Text = AppResources.AddALogin,
                    Command = new Command(() => AddLogin()),
                    Style = (Style)Application.Current.Resources["btn-primaryAccent"]
                };

                NoDataStackLayout.Children.Add(addLoginButton);
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
                LoadFolders(Logins, ct);
            }
            else
            {
                searchFilter = searchFilter.ToLower();
                var filteredLogins = Logins
                    .Where(s => s.Name.ToLower().Contains(searchFilter) || s.Username.ToLower().Contains(searchFilter))
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToArray();

                ct.ThrowIfCancellationRequested();
                LoadFolders(filteredLogins, ct);
            }
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            ListView.ItemSelected += LoginSelected;
            Search.TextChanged += SearchBar_TextChanged;
            Search.SearchButtonPressed += SearchBar_SearchButtonPressed;
            AddLoginItem?.InitEvents();

            _filterResultsCancellationTokenSource = FetchAndLoadVault();

            if(_connectivity.IsConnected && Device.RuntimePlatform == Device.iOS && !_favorites)
            {
                var pushPromptShow = _settings.GetValueOrDefault(Constants.PushInitialPromptShown, false);
                Action registerAction = () =>
                {
                    var lastPushRegistration = _settings.GetValueOrDefault(Constants.PushLastRegistrationDate, DateTime.MinValue);
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
            ListView.ItemSelected -= LoginSelected;
            Search.TextChanged -= SearchBar_TextChanged;
            Search.SearchButtonPressed -= SearchBar_SearchButtonPressed;
            AddLoginItem?.Dispose();
        }

        protected override bool OnBackButtonPressed()
        {
            if(string.IsNullOrWhiteSpace(Uri))
            {
                return false;
            }

            _googleAnalyticsService.TrackExtensionEvent("BackClosed", Uri.StartsWith("http") ? "Website" : "App");
            MessagingCenter.Send(Application.Current, "Autofill", (VaultListPageModel.Login)null);
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
                var loginsTask = _favorites ? _loginService.GetAllAsync(true) : _loginService.GetAllAsync();
                await Task.WhenAll(foldersTask, loginsTask);

                var folders = await foldersTask;
                var logins = await loginsTask;

                Folders = folders
                    .Select(f => new VaultListPageModel.Folder(f))
                    .OrderBy(s => s.Name)
                    .ToArray();

                Logins = logins
                    .Select(s => new VaultListPageModel.Login(s))
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

        private void LoadFolders(VaultListPageModel.Login[] logins, CancellationToken ct)
        {
            var folders = new List<VaultListPageModel.Folder>(Folders);

            foreach(var folder in folders)
            {
                if(folder.Any())
                {
                    folder.Clear();
                }

                var loginsToAdd = logins
                    .Where(s => s.FolderId == folder.Id)
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToList();

                ct.ThrowIfCancellationRequested();
                folder.AddRange(loginsToAdd);
            }

            var noneToAdd = logins
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

        private async void LoginSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var login = e.SelectedItem as VaultListPageModel.Login;
            if(login == null)
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
                var page = new VaultViewLoginPage(login.Id);
                await Navigation.PushForDeviceAsync(page);
            }
            else if(selection == AppResources.Autofill)
            {
                if(_deviceInfoService.Version < 21)
                {
                    MoreClickedAsync(login);
                }
                else
                {
                    _googleAnalyticsService.TrackExtensionEvent("AutoFilled", Uri.StartsWith("http") ? "Website" : "App");
                    MessagingCenter.Send(Application.Current, "Autofill", login);
                }
            }

            ((ListView)sender).SelectedItem = null;
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
            if(!string.IsNullOrWhiteSpace(login.Uri.Value) && (login.Uri.Value.StartsWith("http://")
                || login.Uri.Value.StartsWith("https://")))
            {
                buttons.Add(AppResources.GoToWebsite);
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
            else if(selection == AppResources.GoToWebsite)
            {
                Device.OpenUri(new Uri(login.Uri.Value));
            }
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private async void AddLogin()
        {
            var page = new VaultAddLoginPage(Uri);
            await Navigation.PushForDeviceAsync(page);
        }

        private class AddLoginToolBarItem : ExtendedToolbarItem
        {
            private readonly VaultListLoginsPage _page;

            public AddLoginToolBarItem(VaultListLoginsPage page)
                : base(() => page.AddLogin())
            {
                _page = page;
                Text = AppResources.Add;
                Icon = "plus";
            }
        }

        private class VaultListHeaderViewCell : ExtendedViewCell
        {
            public VaultListHeaderViewCell(VaultListLoginsPage page)
            {
                var image = new CachedImage
                {
                    Source = "folder",
                    WidthRequest = 18,
                    HeightRequest = 18
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

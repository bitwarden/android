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
using System.Collections.Generic;
using System.Threading;
using static Bit.App.Models.Page.VaultListPageModel;

namespace Bit.App.Pages
{
    public class VaultListGroupingsPage : ExtendedContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ICollectionService _collectionService;
        private readonly ICipherService _cipherService;
        private readonly IConnectivity _connectivity;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ISyncService _syncService;
        private readonly IPushNotificationService _pushNotification;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettingsService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private CancellationTokenSource _filterResultsCancellationTokenSource;

        public VaultListGroupingsPage()
            : base(true)
        {
            _folderService = Resolver.Resolve<IFolderService>();
            _collectionService = Resolver.Resolve<ICollectionService>();
            _cipherService = Resolver.Resolve<ICipherService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _syncService = Resolver.Resolve<ISyncService>();
            _pushNotification = Resolver.Resolve<IPushNotificationService>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _settings = Resolver.Resolve<ISettings>();
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ExtendedObservableCollection<Section<GroupingOrCipher>> PresentationSections { get; private set; }
            = new ExtendedObservableCollection<Section<GroupingOrCipher>>();
        public ExtendedListView ListView { get; set; }
        public StackLayout NoDataStackLayout { get; set; }
        public ActivityIndicator LoadingIndicator { get; set; }
        private AddCipherToolBarItem AddCipherItem { get; set; }
        private SearchToolBarItem SearchItem { get; set; }
        public ContentView ContentView { get; set; }
        public Fab Fab { get; set; }
        private static Object lastTappedItem { get; set; }

        private void Init()
        {
            SearchItem = new SearchToolBarItem(this);
            ToolbarItems.Add(SearchItem);

            ListView = new ExtendedListView(ListViewCachingStrategy.RecycleElement)
            {
                IsGroupingEnabled = true,
                ItemsSource = PresentationSections,
                HasUnevenRows = true,
                GroupHeaderTemplate = new DataTemplate(() => new SectionHeaderViewCell(
                    nameof(Section<Grouping>.Name), nameof(Section<Grouping>.Count), new Thickness(16, 12))),
                ItemTemplate = new GroupingOrCipherDataTemplateSelector(this)
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ListView.RowHeight = -1;
            }

            var noDataLabel = new Label
            {
                Text = AppResources.NoItems,
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            var addCipherButton = new ExtendedButton
            {
                Text = AppResources.AddAnItem,
                Command = new Command(() => Helpers.AddCipher(this, null)),
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            NoDataStackLayout = new StackLayout
            {
                Children = { noDataLabel, addCipherButton },
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Padding = new Thickness(20, 0),
                Spacing = 20
            };

            LoadingIndicator = new ActivityIndicator
            {
                IsRunning = true
            };

            if(Device.RuntimePlatform != Device.UWP)
            {
                LoadingIndicator.VerticalOptions = LayoutOptions.CenterAndExpand;
                LoadingIndicator.HorizontalOptions = LayoutOptions.Center;
            }

            ContentView = new ContentView
            {
                Content = LoadingIndicator
            };

            var fabLayout = new FabLayout(ContentView);
            if(Device.RuntimePlatform == Device.Android)
            {
                Fab = new Fab(fabLayout, "plus.png", (sender, args) => Helpers.AddCipher(this, null));
                ListView.BottomPadding = 170;
            }
            else
            {
                AddCipherItem = new AddCipherToolBarItem(this, null);
                ToolbarItems.Add(AddCipherItem);
            }

            Content = fabLayout;
            Title = AppResources.MyVault;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
            {
                if(success)
                {
                    _filterResultsCancellationTokenSource = FetchAndLoadVault();
                }
            });

            ListView.ItemSelected += GroupingOrCipherSelected;
            AddCipherItem?.InitEvents();
            SearchItem?.InitEvents();

            if(lastTappedItem != null)
            {
                ListView.ScrollTo(lastTappedItem, ScrollToPosition.Center, false);
            }
            else
            {
                _filterResultsCancellationTokenSource = FetchAndLoadVault();
            }

            // Push registration
            if(_connectivity.IsConnected)
            {
                var lastPushRegistration = _settings.GetValueOrDefault(Constants.PushLastRegistrationDate,
                    DateTime.MinValue);

                if(Device.RuntimePlatform == Device.iOS)
                {
                    var pushPromptShow = _settings.GetValueOrDefault(Constants.PushInitialPromptShown, false);
                    if(!pushPromptShow)
                    {
                        _settings.AddOrUpdateValue(Constants.PushInitialPromptShown, true);
                        await DisplayAlert(AppResources.EnableAutomaticSyncing, AppResources.PushNotificationAlert,
                            AppResources.OkGotIt);
                    }

                    if(!pushPromptShow || DateTime.UtcNow - lastPushRegistration > TimeSpan.FromDays(1))
                    {
                        _pushNotification.Register();
                    }
                }
                else if(Device.RuntimePlatform == Device.Android &&
                    DateTime.UtcNow - lastPushRegistration > TimeSpan.FromDays(1))
                {
                    _pushNotification.Register();
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            MessagingCenter.Unsubscribe<Application, bool>(Application.Current, "SyncCompleted");

            ListView.ItemSelected -= GroupingOrCipherSelected;
            AddCipherItem?.Dispose();
            SearchItem?.Dispose();
        }

        private CancellationTokenSource FetchAndLoadVault()
        {
            var cts = new CancellationTokenSource();
            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                var sections = new List<Section<GroupingOrCipher>>();
                var favoriteCipherGroupings = new List<GroupingOrCipher>();
                var noFolderCipherGroupings = new List<GroupingOrCipher>();
                var ciphers = await _cipherService.GetAllAsync();
                var collectionsDict = (await _collectionService.GetAllCipherAssociationsAsync())
                    .GroupBy(c => c.Item2).ToDictionary(g => g.Key, v => v.ToList());

                var folderCounts = new Dictionary<string, int>();
                foreach(var cipher in ciphers)
                {
                    if(cipher.Favorite)
                    {
                        favoriteCipherGroupings.Add(new GroupingOrCipher(new Cipher(cipher, _appSettingsService)));
                    }

                    if(cipher.FolderId != null)
                    {
                        if(!folderCounts.ContainsKey(cipher.FolderId))
                        {
                            folderCounts.Add(cipher.FolderId, 0);
                        }
                        folderCounts[cipher.FolderId]++;
                    }
                    else
                    {
                        noFolderCipherGroupings.Add(new GroupingOrCipher(new Cipher(cipher, _appSettingsService)));
                    }
                }

                if(favoriteCipherGroupings?.Any() ?? false)
                {
                    sections.Add(new Section<GroupingOrCipher>(
                        favoriteCipherGroupings.OrderBy(g => g.Cipher.Name).ThenBy(g => g.Cipher.Subtitle).ToList(),
                        AppResources.Favorites));
                }

                var folders = await _folderService.GetAllAsync();
                var collections = await _collectionService.GetAllAsync();

                var folderGroupings = folders?
                    .Select(f => new GroupingOrCipher(new Grouping(f, folderCounts.ContainsKey(f.Id) ? folderCounts[f.Id] : 0)))
                    .OrderBy(g => g.Grouping.Name).ToList();

                if(collections.Any())
                {
                    folderGroupings.Add(new GroupingOrCipher(new Grouping(AppResources.FolderNone,
                        noFolderCipherGroupings.Count)));
                }

                if(folderGroupings.Any())
                {
                    sections.Add(new Section<GroupingOrCipher>(folderGroupings, AppResources.Folders));
                }

                var collectionGroupings = collections?
                    .Select(c => new GroupingOrCipher(new Grouping(c,
                        collectionsDict.ContainsKey(c.Id) ? collectionsDict[c.Id].Count() : 0)))
                   .OrderBy(g => g.Grouping.Name).ToList();
                if(collectionGroupings?.Any() ?? false)
                {
                    sections.Add(new Section<GroupingOrCipher>(collectionGroupings, AppResources.Collections));
                }
                else if(noFolderCipherGroupings?.Any() ?? false)
                {
                    sections.Add(new Section<GroupingOrCipher>(
                        noFolderCipherGroupings.OrderBy(g => g.Cipher.Name).ThenBy(g => g.Cipher.Subtitle).ToList(),
                        AppResources.FolderNone));
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    PresentationSections.ResetWithRange(sections);

                    if(ciphers.Any() || folders.Any())
                    {
                        ContentView.Content = ListView;
                    }
                    else if(_syncService.SyncInProgress)
                    {
                        ContentView.Content = LoadingIndicator;
                    }
                    else
                    {
                        ContentView.Content = NoDataStackLayout;
                    }
                });
            }, cts.Token);

            return cts;
        }

        private async void GroupingOrCipherSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var groupingOrCipher = e.SelectedItem as GroupingOrCipher;
            if(groupingOrCipher == null)
            {
                return;
            }

            if(groupingOrCipher.Grouping != null)
            {
                Page page;
                if(groupingOrCipher.Grouping.Folder)
                {
                    page = new VaultListCiphersPage(folder: true,
                        folderId: groupingOrCipher.Grouping.Id, groupingName: groupingOrCipher.Grouping.Name);
                }
                else
                {
                    page = new VaultListCiphersPage(collectionId: groupingOrCipher.Grouping.Id,
                        groupingName: groupingOrCipher.Grouping.Name);
                }

                await Navigation.PushAsync(page);
            }
            else if(groupingOrCipher.Cipher != null)
            {
                var page = new VaultViewCipherPage(groupingOrCipher.Cipher.Type, groupingOrCipher.Cipher.Id);
                await Navigation.PushForDeviceAsync(page);
            }

            lastTappedItem = e.SelectedItem;
            ((ListView)sender).SelectedItem = null;
        }

        private async void Search()
        {
            var page = new ExtendedNavigationPage(new VaultListCiphersPage());
            await Navigation.PushModalAsync(page);
        }

        private class SearchToolBarItem : ExtendedToolbarItem
        {
            public SearchToolBarItem(VaultListGroupingsPage page)
                : base(() => page.Search())
            {
                Text = AppResources.Search;
                Icon = "search.png";
            }
        }

        public class GroupingOrCipherDataTemplateSelector : DataTemplateSelector
        {
            public GroupingOrCipherDataTemplateSelector(VaultListGroupingsPage page)
            {
                GroupingTemplate = new DataTemplate(() => new VaultGroupingViewCell());
                CipherTemplate = new DataTemplate(() => new VaultListViewCell(
                    (Cipher c) => Helpers.CipherMoreClickedAsync(page, c, false), true));
            }

            public DataTemplate GroupingTemplate { get; set; }
            public DataTemplate CipherTemplate { get; set; }

            protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
            {
                if(item == null)
                {
                    return null;
                }
                return ((GroupingOrCipher)item).Cipher == null ? GroupingTemplate : CipherTemplate;
            }
        }
    }
}

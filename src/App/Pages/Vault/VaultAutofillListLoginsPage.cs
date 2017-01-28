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
using Bit.App.Models;

namespace Bit.App.Pages
{
    public class VaultAutofillListLoginsPage : ExtendedContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ILoginService _loginService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IClipboardService _clipboardService;
        private readonly ISyncService _syncService;
        private readonly IPushNotification _pushNotification;
        private readonly IDeviceInfoService _deviceInfoService;
        private readonly ISettings _settings;
        private CancellationTokenSource _filterResultsCancellationTokenSource;
        private readonly DomainName _domainName;

        public VaultAutofillListLoginsPage(string uriString)
            : base(true)
        {
            Uri uri;
            if(Uri.TryCreate(uriString, UriKind.RelativeOrAbsolute, out uri) &&
                DomainName.TryParse(uri.Host, out _domainName)) { }

            _folderService = Resolver.Resolve<IFolderService>();
            _loginService = Resolver.Resolve<ILoginService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();
            _syncService = Resolver.Resolve<ISyncService>();
            _pushNotification = Resolver.Resolve<IPushNotification>();
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }
        public ExtendedObservableCollection<VaultListPageModel.Login> PresentationLogins { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.Login>();

        public ListView ListView { get; set; }

        private void Init()
        {
            MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
            {
                if(success)
                {
                    _filterResultsCancellationTokenSource = FetchAndLoadVault();
                }
            });

            ToolbarItems.Add(new AddLoginToolBarItem(this));

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                ItemsSource = PresentationLogins,
                HasUnevenRows = true,
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(this))
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                ListView.RowHeight = -1;
            }

            ListView.ItemSelected += LoginSelected;

            Title = AppResources.Logins;

            Content = ListView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _filterResultsCancellationTokenSource = FetchAndLoadVault();
        }

        private CancellationTokenSource FetchAndLoadVault()
        {
            var cts = new CancellationTokenSource();
            _settings.AddOrUpdateValue(Constants.FirstVaultLoad, false);

            if(PresentationLogins.Count > 0 && _syncService.SyncInProgress)
            {
                return cts;
            }

            _filterResultsCancellationTokenSource?.Cancel();

            Task.Run(async () =>
            {
                var logins = await _loginService.GetAllAsync();
                var filteredLogins = logins
                    .Select(s => new VaultListPageModel.Login(s))
                    .Where(s => s.BaseDomain != null && s.BaseDomain == _domainName.BaseDomain)
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username)
                    .ToArray();

                PresentationLogins.ResetWithRange(filteredLogins);
            }, cts.Token);

            return cts;
        }

        private void LoginSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var login = e.SelectedItem as VaultListPageModel.Login;
            MessagingCenter.Send(Application.Current, "Autofill", login);
        }

        private async void AddLogin()
        {
            var page = new VaultAddLoginPage();
            await Navigation.PushForDeviceAsync(page);
        }

        private class AddLoginToolBarItem : ToolbarItem
        {
            private readonly VaultAutofillListLoginsPage _page;

            public AddLoginToolBarItem(VaultAutofillListLoginsPage page)
            {
                _page = page;
                Text = AppResources.Add;
                Icon = "plus";
                Clicked += ClickedItem;
            }

            private void ClickedItem(object sender, EventArgs e)
            {
                _page.AddLogin();
            }
        }

        private class VaultListViewCell : LabeledDetailCell
        {
            private VaultAutofillListLoginsPage _page;

            public static readonly BindableProperty LoginParameterProperty = BindableProperty.Create(nameof(LoginParameter),
                typeof(VaultListPageModel.Login), typeof(VaultListViewCell), null);

            public VaultListViewCell(VaultAutofillListLoginsPage page)
            {
                _page = page;

                SetBinding(LoginParameterProperty, new Binding("."));
                Label.SetBinding<VaultListPageModel.Login>(Label.TextProperty, s => s.Name);
                Detail.SetBinding<VaultListPageModel.Login>(Label.TextProperty, s => s.Username);

                BackgroundColor = Color.White;
            }

            public VaultListPageModel.Login LoginParameter
            {
                get { return GetValue(LoginParameterProperty) as VaultListPageModel.Login; }
                set { SetValue(LoginParameterProperty, value); }
            }
        }
    }
}

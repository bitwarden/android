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
using System.Threading;
using Bit.App.Models;
using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class VaultAutofillListLoginsPage : ExtendedContentPage
    {
        private readonly ILoginService _loginService;
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;
        private CancellationTokenSource _filterResultsCancellationTokenSource;
        private readonly DomainName _domainName;
        private readonly string _uri;
        private readonly string _name;
        private readonly bool _androidApp = false;

        public VaultAutofillListLoginsPage(string uriString)
            : base(true)
        {
            _uri = uriString;
            Uri uri;
            if(!Uri.TryCreate(uriString, UriKind.RelativeOrAbsolute, out uri) ||
                !DomainName.TryParse(uri.Host, out _domainName))
            {
                if(uriString != null && uriString.StartsWith(Constants.AndroidAppProtocol))
                {
                    _androidApp = true;
                    _name = uriString.Substring(Constants.AndroidAppProtocol.Length);
                }
            }
            else
            {
                _name = _domainName.BaseDomain;
            }
            
            _loginService = Resolver.Resolve<ILoginService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            Init();
        }
        public ExtendedObservableCollection<VaultListPageModel.Login> PresentationLogins { get; private set; }
            = new ExtendedObservableCollection<VaultListPageModel.Login>();
        public StackLayout NoDataStackLayout { get; set; }
        public ListView ListView { get; set; }
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

            ToolbarItems.Add(new AddLoginToolBarItem(this));

            ListView = new ListView(ListViewCachingStrategy.RecycleElement)
            {
                ItemsSource = PresentationLogins,
                HasUnevenRows = true,
                ItemTemplate = new DataTemplate(() => new VaultListViewCell(
                    (VaultListPageModel.Login l) => MoreClickedAsync(l)))
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                ListView.RowHeight = -1;
            }

            ListView.ItemSelected += LoginSelected;

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
            _filterResultsCancellationTokenSource = FetchAndLoadVault();
        }

        protected override bool OnBackButtonPressed()
        {
            MessagingCenter.Send(Application.Current, "Autofill", (VaultListPageModel.Login)null);
            return true;
        }

        private void AdjustContent()
        {
            if(PresentationLogins.Count > 0)
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
                var logins = await _loginService.GetAllAsync();
                var filteredLogins = logins
                    .Select(s => new VaultListPageModel.Login(s))
                    .Where(s => (_androidApp && _domainName == null && s.Uri.Value == _uri) ||
                        (_domainName != null && s.BaseDomain != null && s.BaseDomain == _domainName.BaseDomain))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username);

                Device.BeginInvokeOnMainThread(() =>
                {
                    PresentationLogins.ResetWithRange(filteredLogins);
                    AdjustContent();
                });
            }, cts.Token);

            return cts;
        }

        private void LoginSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var login = e.SelectedItem as VaultListPageModel.Login;
            MessagingCenter.Send(Application.Current, "Autofill", login);
        }

        private async void AddLoginAsync()
        {
            var page = new VaultAddLoginPage(_uri, _name);
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
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
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
                _page.AddLoginAsync();
            }
        }
    }
}

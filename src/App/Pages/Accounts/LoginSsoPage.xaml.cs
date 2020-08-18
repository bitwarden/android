using Bit.App.Models;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginSsoPage : BaseContentPage
    {
        private readonly IMessagingService _messagingService;
        private readonly IStorageService _storageService;
        private readonly LoginSsoPageViewModel _vm;
        private readonly AppOptions _appOptions;

        public LoginSsoPage(AppOptions appOptions = null)
        {
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _messagingService.Send("showStatusBar", true);
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as LoginSsoPageViewModel;
            _vm.Page = this;
            _vm.StartTwoFactorAction = () => Device.BeginInvokeOnMainThread(async () => await StartTwoFactorAsync());
            _vm.LoggedInAction = () => Device.BeginInvokeOnMainThread(async () => await LoggedInAsync());
            _vm.CloseAction = async () =>
            {
                _messagingService.Send("showStatusBar", false);
                await Navigation.PopModalAsync();
            };
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }
        
        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if (string.IsNullOrWhiteSpace(_vm.OrgIdentifier))
            {
                RequestFocus(_orgIdentifier);
            }
        }

        private async void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.LogInAsync();
            }
        }
        
        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task StartTwoFactorAsync()
        {
            var page = new TwoFactorPage(true);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
        
        private async Task LoggedInAsync()
        {
            if (_appOptions != null)
            {
                if (_appOptions.FromAutofillFramework && _appOptions.SaveType.HasValue)
                {
                    Application.Current.MainPage = new NavigationPage(new AddEditPage(appOptions: _appOptions));
                    return;
                }
                if (_appOptions.Uri != null)
                {
                    Application.Current.MainPage = new NavigationPage(new AutofillCiphersPage(_appOptions));
                    return;
                }
            }
            var previousPage = await _storageService.GetAsync<PreviousPageInfo>(Constants.PreviousPageKey);
            if (previousPage != null)
            {
                await _storageService.RemoveAsync(Constants.PreviousPageKey);
            }
            Application.Current.MainPage = new LockPage(_appOptions);
        }
    }
}

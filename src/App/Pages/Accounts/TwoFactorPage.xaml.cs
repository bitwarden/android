using Bit.App.Controls;
using Bit.App.Models;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class TwoFactorPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;
        private readonly IStorageService _storageService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly AppOptions _appOptions;

        private TwoFactorPageViewModel _vm;
        private bool _inited;
        private bool _authingWithSso;
        private string _orgIdentifier;

        public TwoFactorPage(bool? authingWithSso = false, AppOptions appOptions = null, string orgIdentifier = null)
        {
            InitializeComponent();
            SetActivityIndicator();
            _authingWithSso = authingWithSso ?? false;
            _appOptions = appOptions;
            _orgIdentifier = orgIdentifier;
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _vm = BindingContext as TwoFactorPageViewModel;
            _vm.Page = this;
            _vm.StartSetPasswordAction = () =>
                Device.BeginInvokeOnMainThread(async () => await StartSetPasswordAsync());
            _vm.TwoFactorAuthSuccessAction = () =>
                Device.BeginInvokeOnMainThread(async () => await TwoFactorAuthSuccessAsync());
            _vm.CloseAction = async () => await Navigation.PopModalAsync();
            DuoWebView = _duoWebView;
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.Remove(_cancelItem);
            }
        }

        public HybridWebView DuoWebView { get; set; }

        public void AddContinueButton()
        {
            if (!ToolbarItems.Contains(_continueItem))
            {
                ToolbarItems.Add(_continueItem);
            }
        }

        public void RemoveContinueButton()
        {
            if (ToolbarItems.Contains(_continueItem))
            {
                ToolbarItems.Remove(_continueItem);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(TwoFactorPage), (message) =>
            {
                if (message.Command == "gotYubiKeyOTP")
                {
                    var token = (string)message.Data;
                    if (_vm.YubikeyMethod && !string.IsNullOrWhiteSpace(token) &&
                        token.Length == 44 && !token.Contains(" "))
                    {
                        Device.BeginInvokeOnMainThread(async () =>
                        {
                            _vm.Token = token;
                            await _vm.SubmitAsync();
                        });
                    }
                }
                else if (message.Command == "resumeYubiKey")
                {
                    if (_vm.YubikeyMethod)
                    {
                        _messagingService.Send("listenYubiKeyOTP", true);
                    }
                }
            });

            await LoadOnAppearedAsync(_scrollView, true, () =>
            {
                if (!_inited)
                {
                    _inited = true;
                    _vm.Init();
                }
                if (_vm.TotpMethod)
                {
                    RequestFocus(_totpEntry);
                } else if (_vm.YubikeyMethod)
                {
                    RequestFocus(_yubikeyTokenEntry);
                }
                return Task.FromResult(0);
            });
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            if (!_vm.YubikeyMethod)
            {
                _messagingService.Send("listenYubiKeyOTP", false);
                _broadcasterService.Unsubscribe(nameof(TwoFactorPage));
            }
        }
        protected override bool OnBackButtonPressed()
        {
            if (_vm.YubikeyMethod)
            {
                _messagingService.Send("listenYubiKeyOTP", false);
                _broadcasterService.Unsubscribe(nameof(TwoFactorPage));
            }
            return base.OnBackButtonPressed();
        }

        private async void Continue_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void Methods_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.AnotherMethodAsync();
            }
        }

        private async void ResendEmail_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SendEmailAsync(true, true);
            }
        }

        private void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private void TryAgain_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                if (_vm.YubikeyMethod)
                {
                    _messagingService.Send("listenYubiKeyOTP", true);
                }
            }
        }

        private async Task StartSetPasswordAsync()
        {
            _vm.CloseAction();
            var page = new SetPasswordPage(_appOptions, _orgIdentifier);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task TwoFactorAuthSuccessAsync()
        {
            if (_authingWithSso)
            {
                Application.Current.MainPage = new NavigationPage(new LockPage(_appOptions));
            }
            else
            {
                if (AppHelpers.SetAlternateMainPage(_appOptions))
                {
                    return;
                }
                var previousPage = await AppHelpers.ClearPreviousPage();
                Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
            }
        }
    }
}

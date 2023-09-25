using System;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class TwoFactorPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;
        private readonly AppOptions _appOptions;

        private TwoFactorPageViewModel _vm;
        private bool _inited;
        private string _orgIdentifier;

        public TwoFactorPage(bool? authingWithSso = false, AppOptions appOptions = null, string orgIdentifier = null)
        {
            InitializeComponent();
            SetActivityIndicator();
            _appOptions = appOptions;
            _orgIdentifier = orgIdentifier;
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _vm = BindingContext as TwoFactorPageViewModel;
            _vm.Page = this;
            _vm.AuthingWithSso = authingWithSso ?? false;
            _vm.StartSetPasswordAction = () =>
                Device.BeginInvokeOnMainThread(async () => await StartSetPasswordAsync());
            _vm.TwoFactorAuthSuccessAction = () =>
                Device.BeginInvokeOnMainThread(async () => await TwoFactorAuthSuccessToMainAsync());
            _vm.LockAction = () =>
                Device.BeginInvokeOnMainThread(TwoFactorAuthSuccessWithSSOLocked);
            _vm.UpdateTempPasswordAction =
                () => Device.BeginInvokeOnMainThread(async () => await UpdateTempPasswordAsync());
            _vm.StartDeviceApprovalOptionsAction =
                () => Device.BeginInvokeOnMainThread(async () => await StartDeviceApprovalOptionsAsync());
            _vm.CloseAction = async () => await Navigation.PopModalAsync();
            DuoWebView = _duoWebView;
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.Remove(_cancelItem);
            }
            if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_moreItem);
            }
            else
            {
                ToolbarItems.Add(_useAnotherTwoStepMethod);
            }
        }

        public HybridWebView DuoWebView { get; set; }

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
                }
                else if (_vm.YubikeyMethod)
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

        private async void TryAgain_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                if (_vm.Fido2Method)
                {
                    await _vm.Fido2AuthenticateAsync();
                }
                else if (_vm.YubikeyMethod)
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

        private async Task UpdateTempPasswordAsync()
        {
            var page = new UpdateTempPasswordPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartDeviceApprovalOptionsAsync()
        {
            var page = new LoginApproveDevicePage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void TwoFactorAuthSuccessWithSSOLocked()
        {
            Application.Current.MainPage = new NavigationPage(new LockPage(_appOptions));
        }

        private async Task TwoFactorAuthSuccessToMainAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();
            Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
        }

        private void Token_TextChanged(object sender, TextChangedEventArgs e)
        {
            _vm.EnableContinue = !string.IsNullOrWhiteSpace(e.NewTextValue);
        }
    }
}

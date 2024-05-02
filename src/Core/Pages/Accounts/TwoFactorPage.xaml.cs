using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

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
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();
            _vm = BindingContext as TwoFactorPageViewModel;
            _vm.Page = this;
            _vm.AuthingWithSso = authingWithSso ?? false;
            _vm.StartSetPasswordAction = () =>
                MainThread.BeginInvokeOnMainThread(async () => await StartSetPasswordAsync());
            _vm.TwoFactorAuthSuccessAction = () =>
                MainThread.BeginInvokeOnMainThread(async () => await TwoFactorAuthSuccessToMainAsync());
            _vm.LockAction = () =>
                MainThread.BeginInvokeOnMainThread(TwoFactorAuthSuccessWithSSOLocked);
            _vm.UpdateTempPasswordAction =
                () => MainThread.BeginInvokeOnMainThread(async () => await UpdateTempPasswordAsync());
            _vm.StartDeviceApprovalOptionsAction =
                () => MainThread.BeginInvokeOnMainThread(async () => await StartDeviceApprovalOptionsAsync());
            _vm.CloseAction = async () => await Navigation.PopModalAsync();
            DuoWebView = _duoWebView;

#if ANDROID
            ToolbarItems.Remove(_cancelItem);
            ToolbarItems.Add(_useAnotherTwoStepMethod);
#else

            ToolbarItems.Add(_moreItem);
#endif
        }

        public HybridWebView DuoWebView { get; set; }

        protected override bool ShouldCheckToPreventOnNavigatedToCalledTwice => true;

        protected override async Task InitOnNavigatedToAsync()
        {
            _broadcasterService.Subscribe(nameof(TwoFactorPage), (message) =>
            {
                if (message.Command == "gotYubiKeyOTP")
                {
                    var token = (string)message.Data;
                    if (_vm.YubikeyMethod && !string.IsNullOrWhiteSpace(token) &&
                        token.Length == 44 && !token.Contains(" "))
                    {
                        MainThread.BeginInvokeOnMainThread(() =>
                        {
                            _vm.Token = token;
                        });
                        _vm.SubmitCommand.Execute(null);
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

        protected override void OnNavigatedFrom(NavigatedFromEventArgs args)
        {
            base.OnNavigatedFrom(args);

            if (!_vm.YubikeyMethod)
            {
                _messagingService.Send("listenYubiKeyOTP", false);
                _broadcasterService.Unsubscribe(nameof(TwoFactorPage));
            }
        }

        private void TwoFactorPage_OnUnloaded(object sender, EventArgs e)
        {
            _duoWebView?.Handler?.DisconnectHandler();
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

        private void Continue_Clicked(object sender, EventArgs e)
        {
            _vm.SubmitCommand.Execute(null);
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
            try
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
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
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
            var page = new LoginApproveDevicePage(_appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void TwoFactorAuthSuccessWithSSOLocked()
        {
            App.MainPage = new NavigationPage(new LockPage(_appOptions));
        }

        private async Task TwoFactorAuthSuccessToMainAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }

            if (_appOptions != null)
            {
                _appOptions.HasJustLoggedInOrUnlocked = true;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();
            App.MainPage = new TabsPage(_appOptions, previousPage);
        }

        private void Token_TextChanged(object sender, TextChangedEventArgs e)
        {
            _vm.EnableContinue = !string.IsNullOrWhiteSpace(e.NewTextValue);
        }
    }
}

using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.Core.Services;

namespace Bit.App.Pages
{
    public partial class LockPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly AppOptions _appOptions;
        private readonly bool _autoPromptBiometric;
        private readonly LockPageViewModel _vm;

        private bool _promptedAfterResume;
        private bool _appeared;

        public LockPage(AppOptions appOptions = null, bool autoPromptBiometric = true, bool checkPendingAuthRequests = true)
        {
            _appOptions = appOptions;
            _autoPromptBiometric = autoPromptBiometric;
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>();
            _vm = BindingContext as LockPageViewModel;
            _vm.CheckPendingAuthRequests = checkPendingAuthRequests;
            _vm.Page = this;
            _vm.UnlockedAction = () => MainThread.BeginInvokeOnMainThread(async () =>
            {
                try
                {
                    await UnlockedAsync();
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                    throw;
                }
            });

#if IOS
            ToolbarItems.Add(_moreItem);
#else
            ToolbarItems.Add(_logOut);
#endif
        }

        public Entry SecretEntry
        {
            get
            {
                if (_vm?.PinEnabled ?? false)
                {
                    return _pin;
                }
                return _masterPassword;
            }
        }

        public async Task PromptBiometricAfterResumeAsync()
        {
            if (_vm.BiometricEnabled)
            {
                await Task.Delay(500);
                if (!_promptedAfterResume)
                {
                    _promptedAfterResume = true;
                    await _vm?.PromptBiometricAsync();
                }
            }
        }

        protected override async void OnAppearing()
        {
            try
            {
                base.OnAppearing();
                _broadcasterService.Subscribe(nameof(LockPage), message =>
                {
                    if (message.Command == Constants.ClearSensitiveFields)
                    {
                        MainThread.BeginInvokeOnMainThread(() => _vm?.ResetPinPasswordFields());
                    }
                });
                if (_appeared)
                {
                    return;
                }

                _appeared = true;
                _mainContent.Content = _mainLayout;

                //Workaround: This delay allows the Avatar to correctly load on iOS. The cause of this issue is also likely connected with the race conditions issue when using loading modals in iOS
                await Task.Delay(50);

                _accountAvatar?.OnAppearing();

                _vm.AvatarImageSource = await GetAvatarImageSourceAsync();

                await _vm.InitAsync();

                _vm.FocusSecretEntry += PerformFocusSecretEntry;

                if (!_vm.BiometricEnabled)
                {
                    RequestFocus(SecretEntry);
                }
                else
                {
                    if (!_vm.HasMasterPassword && !_vm.PinEnabled)
                    {
                        _passwordGrid.IsVisible = false;
                        _unlockButton.IsVisible = false;
                    }
                    if (_autoPromptBiometric)
                    {
                        var tasks = Task.Run(async () =>
                        {
                            await Task.Delay(500);
                            await MainThread.InvokeOnMainThreadAsync(async () => await _vm.PromptBiometricAsync());
                        });
                    }
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        private void PerformFocusSecretEntry(int? cursorPosition)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                SecretEntry.Focus();
                if (cursorPosition.HasValue)
                {
                    SecretEntry.CursorPosition = cursorPosition.Value;
                }
            });
        }

        protected override bool OnBackButtonPressed()
        {
            if (_accountListOverlay.IsVisible)
            {
                _accountListOverlay.HideAsync().FireAndForget();
                return true;
            }
            return false;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            _accountAvatar?.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(LockPage));
        }

        private void Unlock_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                var tasks = Task.Run(async () =>
                {
                    await Task.Delay(50);
                    _vm.SubmitCommand.Execute(null);
                });
            }
        }

        private async void LogOut_Clicked(object sender, EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            if (DoOnce())
            {
                await _vm.LogOutAsync();
            }
        }

        private async void Biometric_Clicked(object sender, EventArgs e)
        {
            try
            {
                if (DoOnce())
                {
                    await _vm.PromptBiometricAsync();
                }

            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        private async void More_Clicked(object sender, System.EventArgs e)
        {
            try
            {
                await _accountListOverlay.HideAsync();

                if (!DoOnce())
                {
                    return;
                }

                var selection = await DisplayActionSheet(AppResources.Options,
                    AppResources.Cancel, null, AppResources.LogOut);

                if (selection == AppResources.LogOut)
                {
                    await _vm.LogOutAsync();
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        private async Task UnlockedAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();

            if (_appOptions != null)
            {
                _appOptions.HasJustLoggedInOrUnlocked = true;
            }
            App.MainPage = new TabsPage(_appOptions, previousPage);
        }
    }
}

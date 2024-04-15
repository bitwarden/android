using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class CipherSelectionPage : BaseContentPage
    {
        private readonly AppOptions _appOptions;
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IAccountsManager _accountsManager;

        private readonly CipherSelectionPageViewModel _vm;

        public CipherSelectionPage(AppOptions appOptions)
        {
            _appOptions = appOptions;

            if (appOptions?.OtpData is null)
            {
                BindingContext = new AutofillCiphersPageViewModel();
            }
            else
            {
                BindingContext = new OTPCipherSelectionPageViewModel();
            }

            InitializeComponent();

#if IOS
            ToolbarItems.Add(_closeItem);
            ToolbarItems.Add(_addItem);
#endif

            SetActivityIndicator(_mainContent);
            _vm = BindingContext as CipherSelectionPageViewModel;
            _vm.Page = this;
            _vm.Init(appOptions);

            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _accountsManager = ServiceContainer.Resolve<IAccountsManager>();
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            if (_syncService.SyncInProgress)
            {
                IsBusy = true;
            }
            if (!await AppHelpers.IsVaultTimeoutImmediateAsync())
            {
                await _vaultTimeoutService.CheckVaultTimeoutAsync();
            }
            if (await _vaultTimeoutService.IsLockedAsync())
            {
                return;
            }

            try
            {
                // don't crash the app if the avatar can't be loaded, just log the ex
                _accountAvatar?.OnAppearing();
                _vm.AvatarImageSource = await GetAvatarImageSourceAsync();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }

            _broadcasterService.Subscribe(nameof(CipherSelectionPage), async (message) =>
            {
                try
                {
                    if (message.Command == "syncStarted")
                    {
                        MainThread.BeginInvokeOnMainThread(() => IsBusy = true);
                    }
                    else if (message.Command == "syncCompleted")
                    {
                        await Task.Delay(500);
                        MainThread.BeginInvokeOnMainThread(() =>
                        {
                            IsBusy = false;
                            if (_vm.LoadedOnce)
                            {
                                var task = _vm.LoadAsync();
                            }
                        });
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });

            await LoadOnAppearedAsync(_mainLayout, false, async () =>
            {
                try
                {
                    await _vm.LoadAsync();
                }
                catch (Exception e) when (e.Message.Contains("No key."))
                {
                    await Task.Delay(1000);
                    await _vm.LoadAsync();
                }
            }, _mainContent);

            UpdatePlaceholder();
        }

        protected override bool OnBackButtonPressed()
        {
            if (_accountListOverlay.IsVisible)
            {
                _accountListOverlay.HideAsync().FireAndForget();
                return true;
            }

#if ANDROID
            _appOptions.Uri = null;

            if (BindingContext is AutofillCiphersPageViewModel autofillVM)
            {
                autofillVM.Cancel();
            }
#endif
            return base.OnBackButtonPressed();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            IsBusy = false;
            _accountAvatar?.OnDisappearing();
        }

        private void AddButton_Clicked(object sender, System.EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }

            if (_vm is AutofillCiphersPageViewModel autofillVM)
            {
                AddFromAutofill(autofillVM).FireAndForget();
            }
        }

        private async Task AddFromAutofill(AutofillCiphersPageViewModel autofillVM)
        {
            if (_appOptions.FillType.HasValue && _appOptions.FillType != CipherType.Login)
            {
                var pageForOther = new CipherAddEditPage(type: _appOptions.FillType, fromAutofill: true);
                await Navigation.PushModalAsync(new NavigationPage(pageForOther));
                return;
            }
            var pageForLogin = new CipherAddEditPage(null, CipherType.Login, uri: autofillVM.Uri, name: _vm.Name,
                fromAutofill: true);
            await Navigation.PushModalAsync(new NavigationPage(pageForLogin));
        }

        private void Search_Clicked(object sender, EventArgs e)
        {
            var page = new CiphersPage(null, appOptions: _appOptions);
            Navigation.PushModalAsync(new NavigationPage(page)).FireAndForget();
        }

        void CloseItem_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _accountsManager.StartDefaultNavigationFlowAsync(op => op.OtpData = null).FireAndForget();

                if (BindingContext is AutofillCiphersPageViewModel autofillVM)
                {
                    autofillVM.Cancel();
                }
            }
        }

        public override async Task UpdateOnThemeChanged()
        {
            await base.UpdateOnThemeChanged();

            UpdatePlaceholder();
        }

        private void UpdatePlaceholder()
        {
#if ANDROID
            MainThread.BeginInvokeOnMainThread(() =>
                _emptyItemsPlaceholder.Source = ImageSource.FromFile(ThemeManager.UsingLightTheme ? "empty_items_state.png" : "empty_items_state_dark.png"));
#endif
        }
    }
}

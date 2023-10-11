using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class GroupingsPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private readonly IPushNotificationService _pushNotificationService;
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ICipherService _cipherService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly GroupingsPageViewModel _vm;
        private readonly string _pageName;

        private PreviousPageInfo _previousPage;

        public GroupingsPage(bool mainPage, CipherType? type = null, string folderId = null,
            string collectionId = null, string pageTitle = null, string vaultFilterSelection = null,
            PreviousPageInfo previousPage = null, bool deleted = false, bool showTotp = false)
        {
            _pageName = string.Concat(nameof(GroupingsPage), "_", DateTime.UtcNow.Ticks);
            InitializeComponent();
            SetActivityIndicator(_mainContent);
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _pushNotificationService = ServiceContainer.Resolve<IPushNotificationService>("pushNotificationService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _vm = BindingContext as GroupingsPageViewModel;
            _vm.Page = this;
            _vm.MainPage = mainPage;
            _vm.Type = type;
            _vm.FolderId = folderId;
            _vm.CollectionId = collectionId;
            _vm.Deleted = deleted;
            _vm.ShowTotp = showTotp;
            _previousPage = previousPage;
            if (pageTitle != null)
            {
                _vm.PageTitle = pageTitle;
            }
            if (vaultFilterSelection != null)
            {
                _vm.VaultFilterDescription = vaultFilterSelection;
            }

            if (Device.RuntimePlatform == Device.iOS)
            {
                _absLayout.Children.Remove(_fab);
                ToolbarItems.Add(_addItem);
            }
            else
            {
                ToolbarItems.Add(_syncItem);
                ToolbarItems.Add(_lockItem);
                ToolbarItems.Add(_exitItem);
            }
            if (deleted || showTotp)
            {
                _absLayout.Children.Remove(_fab);
                ToolbarItems.Remove(_addItem);
            }
            if (!mainPage)
            {
                ToolbarItems.Remove(_accountAvatar);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            if (_syncService.SyncInProgress)
            {
                IsBusy = true;
            }

            _accountAvatar?.OnAppearing();
            if (_vm.MainPage)
            {
                _vm.AvatarImageSource = await GetAvatarImageSourceAsync();
            }

            _broadcasterService.Subscribe(_pageName, async (message) =>
            {
                try
                {
                    if (message.Command == "syncStarted")
                    {
                        Device.BeginInvokeOnMainThread(() => IsBusy = true);
                    }
                    else if (message.Command == "syncCompleted")
                    {
                        await Task.Delay(500);
                        if (_vm.MainPage)
                        {
                            _vm.AvatarImageSource = await GetAvatarImageSourceAsync();
                        }
                        Device.BeginInvokeOnMainThread(() =>
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
                if (_previousPage == null)
                {
                    if (!_syncService.SyncInProgress || (await _cipherService.GetAllAsync()).Any())
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
                    }
                    else
                    {
                        await Task.Delay(5000);
                        if (!_vm.Loaded)
                        {
                            await _vm.LoadAsync();
                        }
                    }
                }
                await ShowPreviousPageAsync();
                AdjustToolbar();
            }, _mainContent);

            if (!_vm.MainPage)
            {
                return;
            }

            // Push registration
            var lastPushRegistration = await _stateService.GetPushLastRegistrationDateAsync();
            lastPushRegistration = lastPushRegistration.GetValueOrDefault(DateTime.MinValue);
            if (Device.RuntimePlatform == Device.iOS)
            {
                var pushPromptShow = await _stateService.GetPushInitialPromptShownAsync();
                if (!pushPromptShow.GetValueOrDefault(false))
                {
                    await _stateService.SetPushInitialPromptShownAsync(true);
                    await DisplayAlert(AppResources.EnableAutomaticSyncing, AppResources.PushNotificationAlert,
                        AppResources.OkGotIt);
                }
                if (!pushPromptShow.GetValueOrDefault(false) ||
                    DateTime.UtcNow - lastPushRegistration > TimeSpan.FromDays(1))
                {
                    await _pushNotificationService.RegisterAsync();
                }
            }
            else if (Device.RuntimePlatform == Device.Android)
            {
                if (DateTime.UtcNow - lastPushRegistration > TimeSpan.FromDays(1))
                {
                    await _pushNotificationService.RegisterAsync();
                }
            }
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

        protected override async void OnDisappearing()
        {
            base.OnDisappearing();
            IsBusy = false;
            _vm.StopCiphersTotpTick().FireAndForget();
            _broadcasterService.Unsubscribe(_pageName);
            _vm.DisableRefreshing();
            _accountAvatar?.OnDisappearing();
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            try
            {
                ((ExtendedCollectionView)sender).SelectedItem = null;
                if (!DoOnce())
                {
                    return;
                }

                if (e.CurrentSelection?.FirstOrDefault() is GroupingsPageTOTPListItem totpItem)
                {
                    await _vm.SelectCipherAsync(totpItem.Cipher);
                    return;
                }

                if (!(e.CurrentSelection?.FirstOrDefault() is GroupingsPageListItem item))
                {
                    return;
                }

                if (item.IsTrash)
                {
                    await _vm.SelectTrashAsync();
                }
                else if (item.IsTotpCode)
                {
                    await _vm.SelectTotpCodesAsync();
                }
                else if (item.Cipher != null)
                {
                    await _vm.SelectCipherAsync(item.Cipher);
                }
                else if (item.Folder != null)
                {
                    await _vm.SelectFolderAsync(item.Folder);
                }
                else if (item.Collection != null)
                {
                    await _vm.SelectCollectionAsync(item.Collection);
                }
                else if (item.Type != null)
                {
                    await _vm.SelectTypeAsync(item.Type.Value);
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                _platformUtilsService.ShowDialogAsync(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok).FireAndForget();
            }
        }

        private async void Search_Clicked(object sender, EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            if (DoOnce())
            {
                var page = new CiphersPage(_vm.Filter, _vm.MainPage ? null : _vm.PageTitle, deleted: _vm.Deleted);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Sync_Clicked(object sender, EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            await _vm.SyncAsync();
        }

        private async void Lock_Clicked(object sender, EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            await _vaultTimeoutService.LockAsync(true, true);
        }

        private async void Exit_Clicked(object sender, EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            await _vm.ExitAsync();
        }

        private async void AddButton_Clicked(object sender, EventArgs e)
        {
            var skipAction = _accountListOverlay.IsVisible && Device.RuntimePlatform == Device.Android;
            await _accountListOverlay.HideAsync();
            if (skipAction)
            {
                // Account list in the process of closing via tapping on invisible FAB, skip this attempt
                return;
            }
            if (!_vm.Deleted && DoOnce())
            {
                var page = new CipherAddEditPage(null, _vm.Type, _vm.FolderId, _vm.CollectionId, _vm.GetVaultFilterOrgId());
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async Task ShowPreviousPageAsync()
        {
            if (_previousPage == null)
            {
                return;
            }
            await _accountListOverlay.HideAsync();
            if (_previousPage.Page == "view" && !string.IsNullOrWhiteSpace(_previousPage.CipherId))
            {
                await Navigation.PushModalAsync(new NavigationPage(new CipherDetailsPage(_previousPage.CipherId)));
            }
            else if (_previousPage.Page == "edit" && !string.IsNullOrWhiteSpace(_previousPage.CipherId))
            {
                await Navigation.PushModalAsync(new NavigationPage(new CipherAddEditPage(_previousPage.CipherId)));
            }
            _previousPage = null;
        }

        private void AdjustToolbar()
        {
            _addItem.IsEnabled = !_vm.Deleted;
            _addItem.IconImageSource = _vm.Deleted ? null : "plus.png";
        }

        public async Task HideAccountSwitchingOverlayAsync()
        {
            await _accountListOverlay.HideAsync();
        }
    }
}

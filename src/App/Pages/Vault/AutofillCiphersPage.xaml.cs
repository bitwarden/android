using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AutofillCiphersPage : BaseContentPage
    {
        private readonly AppOptions _appOptions;
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private readonly IVaultTimeoutService _vaultTimeoutService;

        private AutofillCiphersPageViewModel _vm;

        public AutofillCiphersPage(AppOptions appOptions)
        {
            _appOptions = appOptions;
            InitializeComponent();
            SetActivityIndicator(_mainContent);
            _vm = BindingContext as AutofillCiphersPageViewModel;
            _vm.Page = this;
            _vm.Init(appOptions);

            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
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

            _accountAvatar?.OnAppearing();
            _vm.AvatarImageSource = await GetAvatarImageSourceAsync();

            _broadcasterService.Subscribe(nameof(AutofillCiphersPage), async (message) =>
            {
                if (message.Command == "syncStarted")
                {
                    Device.BeginInvokeOnMainThread(() => IsBusy = true);
                }
                else if (message.Command == "syncCompleted")
                {
                    await Task.Delay(500);
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        IsBusy = false;
                        if (_vm.LoadedOnce)
                        {
                            var task = _vm.LoadAsync();
                        }
                    });
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
        }

        protected override bool OnBackButtonPressed()
        {
            if (_accountListOverlay.IsVisible)
            {
                _accountListOverlay.HideAsync().FireAndForget();
                return true;
            }
            if (Device.RuntimePlatform == Device.Android)
            {
                _appOptions.Uri = null;
            }
            return base.OnBackButtonPressed();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            IsBusy = false;
            _accountAvatar?.OnDisappearing();
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            ((ExtendedCollectionView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }
            if (e.CurrentSelection?.FirstOrDefault() is GroupingsPageListItem item && item.Cipher != null)
            {
                await _vm.SelectCipherAsync(item.Cipher, item.FuzzyAutofill);
            }
        }

        private async void AddButton_Clicked(object sender, System.EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            if (_appOptions.FillType.HasValue && _appOptions.FillType != CipherType.Login)
            {
                var pageForOther = new AddEditPage(type: _appOptions.FillType, fromAutofill: true);
                await Navigation.PushModalAsync(new NavigationPage(pageForOther));
                return;
            }
            var pageForLogin = new AddEditPage(null, CipherType.Login, uri: _vm.Uri, name: _vm.Name,
                fromAutofill: true);
            await Navigation.PushModalAsync(new NavigationPage(pageForLogin));
        }

        private void Search_Clicked(object sender, System.EventArgs e)
        {
            var page = new CiphersPage(null, autofillUrl: _vm.Uri);
            Application.Current.MainPage = new NavigationPage(page);
        }
    }
}

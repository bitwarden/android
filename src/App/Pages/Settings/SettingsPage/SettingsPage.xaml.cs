using System;
using System.ComponentModel;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Pages.Accounts;
using Bit.App.Resources;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class SettingsPage : BaseContentPage
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly TabsPage _tabsPage;
        private SettingsPageViewModel _vm;

        public SettingsPage(TabsPage tabsPage)
        {
            _tabsPage = tabsPage;
            InitializeComponent();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _vm = BindingContext as SettingsPageViewModel;
            _vm.Page = this;
        }

        public async Task InitAsync()
        {
            await _vm.InitAsync();
        }

        public void BuildList()
        {
            _vm.BuildList();
        }

        protected override bool OnBackButtonPressed()
        {
            if (Device.RuntimePlatform == Device.Android && _tabsPage != null)
            {
                _tabsPage.ResetToVaultPage();
                return true;
            }
            return base.OnBackButtonPressed();
        }

        void ActivateTimePicker(object sender, EventArgs args)
        {
            var stackLayout = (ExtendedStackLayout)sender;
            SettingsPageListItem item = (SettingsPageListItem)stackLayout.BindingContext;
            if (item.ShowTimeInput)
            {
                var timePicker = stackLayout.Children.Where(x => x is TimePicker).FirstOrDefault();
                ((TimePicker)timePicker)?.Focus();
            }
        }

        async void OnTimePickerPropertyChanged(object sender, PropertyChangedEventArgs args)
        {
            var s = (TimePicker)sender;
            var time = s.Time.TotalMinutes;
            if (s.IsFocused && args.PropertyName == "Time")
            {
                await _vm.VaultTimeoutAsync(false, (int)time);
            }
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            ((ExtendedCollectionView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }
            if (!(e.CurrentSelection?.FirstOrDefault() is SettingsPageListItem item))
            {
                return;
            }

            if (item.Name == AppResources.Sync)
            {
                await Navigation.PushModalAsync(new NavigationPage(new SyncPage()));
            }
            else if (item.Name == AppResources.AutofillServices)
            {
                await Navigation.PushModalAsync(new NavigationPage(new AutofillServicesPage(this)));
            }
            else if (item.Name == AppResources.PasswordAutofill)
            {
                await Navigation.PushModalAsync(new NavigationPage(new AutofillPage()));
            }
            else if (item.Name == AppResources.AppExtension)
            {
                await Navigation.PushModalAsync(new NavigationPage(new ExtensionPage()));
            }
            else if (item.Name == AppResources.Options)
            {
                await Navigation.PushModalAsync(new NavigationPage(new OptionsPage()));
            }
            else if (item.Name == AppResources.Folders)
            {
                await Navigation.PushModalAsync(new NavigationPage(new FoldersPage()));
            }
            else if (item.Name == AppResources.About)
            {
                await _vm.AboutAsync();
            }
            else if (item.Name == AppResources.HelpAndFeedback)
            {
                _vm.Help();
            }
            else if (item.Name == AppResources.FingerprintPhrase)
            {
                await _vm.FingerprintAsync();
            }
            else if (item.Name == AppResources.RateTheApp)
            {
                _vm.Rate();
            }
            else if (item.Name == AppResources.ImportItems)
            {
                _vm.Import();
            }
            else if (item.Name == AppResources.ExportVault)
            {
                await Navigation.PushModalAsync(new NavigationPage(new ExportVaultPage()));
            }
            else if (item.Name == AppResources.LearnOrg)
            {
                await _vm.ShareAsync();
            }
            else if (item.Name == AppResources.WebVault)
            {
                _vm.WebVault();
            }
            else if (item.Name == AppResources.ChangeMasterPassword)
            {
                await _vm.ChangePasswordAsync();
            }
            else if (item.Name == AppResources.TwoStepLogin)
            {
                await _vm.TwoStepAsync();
            }
            else if (item.Name == AppResources.LogOut)
            {
                await _vm.LogOutAsync();
            }
            else if (item.Name == AppResources.DeleteAccount)
            {
                await Navigation.PushModalAsync(new NavigationPage(new DeleteAccountPage()));
            }
            else if (item.Name == AppResources.LockNow)
            {
                await _vm.LockAsync();
            }
            else if (item.Name == AppResources.VaultTimeout)
            {
                await _vm.VaultTimeoutAsync();
            }
            else if (item.Name == AppResources.VaultTimeoutAction)
            {
                await _vm.VaultTimeoutActionAsync();
            }
            else if (item.Name == AppResources.UnlockWithPIN)
            {
                await _vm.UpdatePinAsync();
            }
            else
            {
                var biometricName = AppResources.Biometrics;
                if (Device.RuntimePlatform == Device.iOS)
                {
                    var supportsFace = await _deviceActionService.SupportsFaceBiometricAsync();
                    biometricName = supportsFace ? AppResources.FaceID : AppResources.TouchID;
                }
                if (item.Name == string.Format(AppResources.UnlockWith, biometricName))
                {
                    await _vm.UpdateBiometricAsync();
                }
            }
        }
    }
}

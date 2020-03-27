using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Utilities;
using System.Threading.Tasks;
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

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            ((ListView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }
            if (!(e.SelectedItem is SettingsPageListItem item))
            {
                return;
            }

            if (item.Name == AppResources.Sync)
            {
                await Navigation.PushModalAsync(new NavigationPage(new SyncPage()));
            }
            else if (item.Name == AppResources.AutofillAccessibilityService)
            {
                await Navigation.PushModalAsync(new NavigationPage(new AccessibilityServicePage(this)));
            }
            else if (item.Name == AppResources.AutofillService)
            {
                await Navigation.PushModalAsync(new NavigationPage(new AutofillServicePage(this)));
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
            else if (item.Name == AppResources.ShareVault)
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
            else if (item.Name == AppResources.LockNow)
            {
                await _vm.LockAsync();
            }
            else if (item.Name == AppResources.LockOptions)
            {
                await _vm.LockOptionsAsync();
            }
            else if (item.Name == AppResources.UnlockWithPIN)
            {
                await _vm.UpdatePinAsync();
            }
            else
            {
                var fingerprintName = AppResources.Fingerprint;
                if (Device.RuntimePlatform == Device.iOS)
                {
                    var supportsFace = await _deviceActionService.SupportsFaceBiometricAsync();
                    fingerprintName = supportsFace ? AppResources.FaceID : AppResources.TouchID;
                }
                else if (Device.RuntimePlatform == Device.Android && _deviceActionService.UseNativeBiometric())
                {
                    fingerprintName = AppResources.Biometrics;
                }
                if (item.Name == string.Format(AppResources.UnlockWith, fingerprintName))
                {
                    await _vm.UpdateFingerprintAsync();
                }
            }
        }
    }
}

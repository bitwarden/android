using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AddEditPage : BaseContentPage
    {
        private readonly AppOptions _appOptions;
        private readonly IStorageService _storageService;
        private readonly IDeviceActionService _deviceActionService;

        private AddEditPageViewModel _vm;
        private bool _fromAutofill;

        public AddEditPage(
            string cipherId = null,
            CipherType? type = null,
            string folderId = null,
            string collectionId = null,
            string name = null,
            string uri = null,
            bool fromAutofill = false,
            AppOptions appOptions = null)
        {
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _appOptions = appOptions;
            _fromAutofill = fromAutofill;
            FromAutofillFramework = _appOptions?.FromAutofillFramework ?? false;
            InitializeComponent();
            _vm = BindingContext as AddEditPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            _vm.FolderId = folderId;
            _vm.CollectionIds = collectionId != null ? new HashSet<string>(new List<string> { collectionId }) : null;
            _vm.Type = type;
            _vm.DefaultName = name ?? appOptions?.SaveName;
            _vm.DefaultUri = uri ?? appOptions?.Uri;
            _vm.Init();
            SetActivityIndicator();
            if(!_vm.EditMode)
            {
                ToolbarItems.Remove(_attachmentsItem);
                ToolbarItems.Remove(_deleteItem);
            }

            _typePicker.ItemDisplayBinding = new Binding("Key");
            _cardBrandPicker.ItemDisplayBinding = new Binding("Key");
            _cardExpMonthPicker.ItemDisplayBinding = new Binding("Key");
            _identityTitlePicker.ItemDisplayBinding = new Binding("Key");
            _folderPicker.ItemDisplayBinding = new Binding("Key");
            _ownershipPicker.ItemDisplayBinding = new Binding("Key");

            _nameEntry.ReturnType = ReturnType.Next;
            _nameEntry.ReturnCommand = new Command(() =>
            {
                if(_vm.Cipher.Type == CipherType.Login)
                {
                    _loginUsernameEntry.Focus();
                }
                else if(_vm.Cipher.Type == CipherType.Card)
                {
                    _cardholderNameEntry.Focus();
                }
            });

            _loginUsernameEntry.ReturnType = ReturnType.Next;
            _loginUsernameEntry.ReturnCommand = new Command(() => _loginPasswordEntry.Focus());
            _loginPasswordEntry.ReturnType = ReturnType.Next;
            _loginPasswordEntry.ReturnCommand = new Command(() => _loginTotpEntry.Focus());

            _cardholderNameEntry.ReturnType = ReturnType.Next;
            _cardholderNameEntry.ReturnCommand = new Command(() => _cardNumberEntry.Focus());
            _cardExpYearEntry.ReturnType = ReturnType.Next;
            _cardExpYearEntry.ReturnCommand = new Command(() => _cardCodeEntry.Focus());

            _identityFirstNameEntry.ReturnType = ReturnType.Next;
            _identityFirstNameEntry.ReturnCommand = new Command(() => _identityMiddleNameEntry.Focus());
            _identityMiddleNameEntry.ReturnType = ReturnType.Next;
            _identityMiddleNameEntry.ReturnCommand = new Command(() => _identityLastNameEntry.Focus());
            _identityLastNameEntry.ReturnType = ReturnType.Next;
            _identityLastNameEntry.ReturnCommand = new Command(() => _identityUsernameEntry.Focus());
            _identityUsernameEntry.ReturnType = ReturnType.Next;
            _identityUsernameEntry.ReturnCommand = new Command(() => _identityCompanyEntry.Focus());
            _identityCompanyEntry.ReturnType = ReturnType.Next;
            _identityCompanyEntry.ReturnCommand = new Command(() => _identitySsnEntry.Focus());
            _identitySsnEntry.ReturnType = ReturnType.Next;
            _identitySsnEntry.ReturnCommand = new Command(() => _identityPassportNumberEntry.Focus());
            _identityPassportNumberEntry.ReturnType = ReturnType.Next;
            _identityPassportNumberEntry.ReturnCommand = new Command(() => _identityLicenseNumberEntry.Focus());
            _identityLicenseNumberEntry.ReturnType = ReturnType.Next;
            _identityLicenseNumberEntry.ReturnCommand = new Command(() => _identityEmailEntry.Focus());
            _identityEmailEntry.ReturnType = ReturnType.Next;
            _identityEmailEntry.ReturnCommand = new Command(() => _identityPhoneEntry.Focus());
            _identityPhoneEntry.ReturnType = ReturnType.Next;
            _identityPhoneEntry.ReturnCommand = new Command(() => _identityAddress1Entry.Focus());
            _identityAddress1Entry.ReturnType = ReturnType.Next;
            _identityAddress1Entry.ReturnCommand = new Command(() => _identityAddress2Entry.Focus());
            _identityAddress2Entry.ReturnType = ReturnType.Next;
            _identityAddress2Entry.ReturnCommand = new Command(() => _identityAddress3Entry.Focus());
            _identityAddress3Entry.ReturnType = ReturnType.Next;
            _identityAddress3Entry.ReturnCommand = new Command(() => _identityCityEntry.Focus());
            _identityCityEntry.ReturnType = ReturnType.Next;
            _identityCityEntry.ReturnCommand = new Command(() => _identityStateEntry.Focus());
            _identityStateEntry.ReturnType = ReturnType.Next;
            _identityStateEntry.ReturnCommand = new Command(() => _identityPostalCodeEntry.Focus());
            _identityPostalCodeEntry.ReturnType = ReturnType.Next;
            _identityPostalCodeEntry.ReturnCommand = new Command(() => _identityCountryEntry.Focus());
        }

        public bool FromAutofillFramework { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, async () =>
            {
                var success = await _vm.LoadAsync(_appOptions);
                if(!success)
                {
                    await Navigation.PopModalAsync();
                    return;
                }
                AdjustToolbar();
                await ShowAlertsAsync();
                if(!_vm.EditMode && string.IsNullOrWhiteSpace(_vm.Cipher?.Name))
                {
                    RequestFocus(_nameEntry);
                }
            });
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
        }

        protected override bool OnBackButtonPressed()
        {
            if(FromAutofillFramework)
            {
                Application.Current.MainPage = new TabsPage();
                return true;
            }
            return base.OnBackButtonPressed();
        }

        private async void PasswordHistory_Tapped(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await Navigation.PushModalAsync(new NavigationPage(new PasswordHistoryPage(_vm.CipherId)));
            }
        }

        private async void Save_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private void NewUri_Clicked(object sender, System.EventArgs e)
        {
            _vm.AddUri();
        }

        private void NewField_Clicked(object sender, System.EventArgs e)
        {
            _vm.AddField();
        }

        private async void Attachments_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new AttachmentsPage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Share_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new SharePage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Delete_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                if(await _vm.DeleteAsync())
                {
                    await Navigation.PopModalAsync();
                }
            }
        }

        private async void Collections_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new CollectionsPage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void ScanTotp_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new ScanPage(key =>
                {
                    Device.BeginInvokeOnMainThread(async () =>
                    {
                        await Navigation.PopModalAsync();
                        await _vm.UpdateTotpKeyAsync(key);
                    });
                });
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async Task ShowAlertsAsync()
        {
            if(!_vm.EditMode)
            {
                if(_vm.Cipher == null)
                {
                    return;
                }
                var addLoginShown = await _storageService.GetAsync<bool?>(Constants.AddSitePromptShownKey);
                if(_vm.Cipher.Type == CipherType.Login && !_fromAutofill && !addLoginShown.GetValueOrDefault())
                {
                    await _storageService.SaveAsync(Constants.AddSitePromptShownKey, true);
                    if(Device.RuntimePlatform == Device.iOS)
                    {
                        if(_deviceActionService.SystemMajorVersion() < 12)
                        {
                            await DisplayAlert(AppResources.BitwardenAppExtension,
                                AppResources.BitwardenAppExtensionAlert2, AppResources.Ok);
                        }
                        else
                        {
                            await DisplayAlert(AppResources.PasswordAutofill,
                                AppResources.BitwardenAutofillAlert2, AppResources.Ok);
                        }
                    }
                    else if(Device.RuntimePlatform == Device.Android &&
                        !_deviceActionService.AutofillAccessibilityServiceRunning() &&
                        !_deviceActionService.AutofillServiceEnabled())
                    {
                        await DisplayAlert(AppResources.BitwardenAutofillService,
                            AppResources.BitwardenAutofillServiceAlert2, AppResources.Ok);
                    }
                }
            }
        }

        private void AdjustToolbar()
        {
            if(_vm.EditMode && Device.RuntimePlatform == Device.Android)
            {
                if(_vm.Cipher == null)
                {
                    return;
                }
                if(_vm.Cipher.OrganizationId == null)
                {
                    if(ToolbarItems.Contains(_collectionsItem))
                    {
                        ToolbarItems.Remove(_collectionsItem);
                    }
                    if(!ToolbarItems.Contains(_shareItem))
                    {
                        ToolbarItems.Insert(2, _shareItem);
                    }
                }
                else
                {
                    if(ToolbarItems.Contains(_shareItem))
                    {
                        ToolbarItems.Remove(_shareItem);
                    }
                    if(!ToolbarItems.Contains(_collectionsItem))
                    {
                        ToolbarItems.Insert(2, _collectionsItem);
                    }
                }
            }
        }
    }
}

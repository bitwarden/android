using Bit.App.Models;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LockPage : BaseContentPage
    {
        private readonly IStorageService _storageService;
        private readonly AppOptions _appOptions;
        private readonly bool _autoPromptFingerprint;
        private readonly LockPageViewModel _vm;

        private bool _promptedAfterResume;
        private bool _appeared;

        public LockPage(AppOptions appOptions = null, bool autoPromptFingerprint = true)
        {
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _appOptions = appOptions;
            _autoPromptFingerprint = autoPromptFingerprint;
            InitializeComponent();
            _vm = BindingContext as LockPageViewModel;
            _vm.Page = this;
            _vm.UnlockedAction = () => Device.BeginInvokeOnMainThread(async () => await UnlockedAsync());
            MasterPasswordEntry = _masterPassword;
            PinEntry = _pin;
        }

        public Entry MasterPasswordEntry { get; set; }
        public Entry PinEntry { get; set; }

        public async Task PromptFingerprintAfterResumeAsync()
        {
            if(_vm.FingerprintLock)
            {
                await Task.Delay(500);
                if(!_promptedAfterResume)
                {
                    _promptedAfterResume = true;
                    await _vm?.PromptFingerprintAsync();
                }
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            if(_appeared)
            {
                return;
            }
            _appeared = true;
            await _vm.InitAsync(_autoPromptFingerprint);
            if(!_vm.FingerprintLock)
            {
                if(_vm.PinLock)
                {
                    RequestFocus(PinEntry);
                }
                else
                {
                    RequestFocus(MasterPasswordEntry);
                }
            }
        }

        private void Unlock_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                var tasks = Task.Run(async () =>
                {
                    await Task.Delay(50);
                    Device.BeginInvokeOnMainThread(async () => await _vm.SubmitAsync());
                });
            }
        }

        private async void LogOut_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.LogOutAsync();
            }
        }

        private async void Fingerprint_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.PromptFingerprintAsync();
            }
        }

        private async Task UnlockedAsync()
        {
            if(_appOptions != null)
            {
                if(_appOptions.FromAutofillFramework && _appOptions.SaveType.HasValue)
                {
                    Application.Current.MainPage = new NavigationPage(new AddEditPage(appOptions: _appOptions));
                    return;
                }
                else if(_appOptions.Uri != null)
                {
                    Application.Current.MainPage = new NavigationPage(new AutofillCiphersPage(_appOptions));
                    return;
                }
            }
            var previousPage = await _storageService.GetAsync<PreviousPageInfo>(Constants.PreviousPageKey);
            if(previousPage != null)
            {
                await _storageService.RemoveAsync(Constants.PreviousPageKey);
            }
            Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
        }
    }
}

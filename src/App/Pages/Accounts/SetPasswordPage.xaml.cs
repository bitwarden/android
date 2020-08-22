using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.Core;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class SetPasswordPage : BaseContentPage
    {
        private readonly IStorageService _storageService;
        private readonly IMessagingService _messagingService;
        private readonly SetPasswordPageViewModel _vm;
        private readonly AppOptions _appOptions;

        public SetPasswordPage(AppOptions appOptions = null)
        {
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _messagingService.Send("showStatusBar", true);
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as SetPasswordPageViewModel;
            _vm.Page = this;
            _vm.SetPasswordSuccessAction =
                () => Device.BeginInvokeOnMainThread(async () => await SetPasswordSuccessAsync());
            _vm.CloseAction = async () =>
            {
                _messagingService.Send("showStatusBar", false);
                await Navigation.PopModalAsync();
            };
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }

            MasterPasswordEntry = _masterPassword;
            ConfirmMasterPasswordEntry = _confirmMasterPassword;

            _masterPassword.ReturnType = ReturnType.Next;
            _masterPassword.ReturnCommand = new Command(() => _confirmMasterPassword.Focus());
            _confirmMasterPassword.ReturnType = ReturnType.Next;
            _confirmMasterPassword.ReturnCommand = new Command(() => _hint.Focus());
        }

        public Entry MasterPasswordEntry { get; set; }
        public Entry ConfirmMasterPasswordEntry { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            RequestFocus(_masterPassword);
        }

        private async void Submit_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task SetPasswordSuccessAsync()
        {
            if (_appOptions != null)
            {
                if (_appOptions.FromAutofillFramework && _appOptions.SaveType.HasValue)
                {
                    Application.Current.MainPage = new NavigationPage(new AddEditPage(appOptions: _appOptions));
                    return;
                }
                if (_appOptions.Uri != null)
                {
                    Application.Current.MainPage = new NavigationPage(new AutofillCiphersPage(_appOptions));
                    return;
                }
            }
            var previousPage = await _storageService.GetAsync<PreviousPageInfo>(Constants.PreviousPageKey);
            if (previousPage != null)
            {
                await _storageService.RemoveAsync(Constants.PreviousPageKey);
            }
            Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
        }
    }
}

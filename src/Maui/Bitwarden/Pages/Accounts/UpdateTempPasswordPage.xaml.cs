using System;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class UpdateTempPasswordPage : BaseContentPage
    {
        private readonly IMessagingService _messagingService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly UpdateTempPasswordPageViewModel _vm;
        private readonly string _pageName;

        public UpdateTempPasswordPage()
        {
            // Service Init
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");

            // Binding
            InitializeComponent();
            _pageName = string.Concat(nameof(UpdateTempPasswordPage), "_", DateTime.UtcNow.Ticks);
            _vm = BindingContext as UpdateTempPasswordPageViewModel;
            _vm.Page = this;
            SetActivityIndicator();

            // Actions Declaration
            _vm.LogOutAction = () =>
            {
                _messagingService.Send("logout");
            };
            _vm.UpdateTempPasswordSuccessAction = () => Device.BeginInvokeOnMainThread(UpdateTempPasswordSuccess);

            // Link fields that will be referenced in codebehind
            MasterPasswordEntry = _masterPassword;
            ConfirmMasterPasswordEntry = _confirmMasterPassword;

            // Return Types and Commands
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
            await LoadOnAppearedAsync(_mainLayout, true, async () =>
            {
                await _vm.InitAsync(true);
            });
            RequestFocus(_masterPassword);
        }

        private async void Submit_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void LogOut_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation,
                    AppResources.LogOut, AppResources.Yes, AppResources.Cancel);
                if (confirmed)
                {
                    _vm.LogOutAction();
                }
            }
        }

        private void UpdateTempPasswordSuccess()
        {
            _messagingService.Send("logout");
        }
    }
}

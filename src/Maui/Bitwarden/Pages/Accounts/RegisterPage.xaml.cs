using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class RegisterPage : BaseContentPage
    {
        private readonly RegisterPageViewModel _vm;

        private bool _inputFocused;

        public RegisterPage(HomePage homePage)
        {
            InitializeComponent();
            _vm = BindingContext as RegisterPageViewModel;
            _vm.Page = this;
            _vm.RegistrationSuccess = () => Device.BeginInvokeOnMainThread(async () => await RegistrationSuccessAsync(homePage));
            _vm.CloseAction = async () =>
            {
                await Navigation.PopModalAsync();
            };
            MasterPasswordEntry = _masterPassword;
            ConfirmMasterPasswordEntry = _confirmMasterPassword;
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }

            _email.ReturnType = ReturnType.Next;
            _email.ReturnCommand = new Command(() => _masterPassword.Focus());
            _masterPassword.ReturnType = ReturnType.Next;
            _masterPassword.ReturnCommand = new Command(() => _confirmMasterPassword.Focus());
            _confirmMasterPassword.ReturnType = ReturnType.Next;
            _confirmMasterPassword.ReturnCommand = new Command(() => _hint.Focus());
        }

        public Entry MasterPasswordEntry { get; set; }
        public Entry ConfirmMasterPasswordEntry { get; set; }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if (!_inputFocused)
            {
                RequestFocus(_email);
                _inputFocused = true;
            }
        }

        private async void Submit_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async Task RegistrationSuccessAsync(HomePage homePage)
        {
            if (homePage != null)
            {
                await homePage.DismissRegisterPageAndLogInAsync(_vm.Email);
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }
    }
}

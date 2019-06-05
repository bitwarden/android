using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class RegisterPage : BaseContentPage
    {
        private RegisterPageViewModel _vm;

        public RegisterPage(HomePage homePage)
        {
            InitializeComponent();
            _vm = BindingContext as RegisterPageViewModel;
            _vm.Page = this;
            _vm.RegistrationSuccess = async () =>
            {
                if(homePage != null)
                {
                    await homePage.DismissRegisterPageAndLogInAsync(_vm.Email);
                }
            };
            MasterPasswordEntry = _masterPassword;
            ConfirmMasterPasswordEntry = _confirmMasterPassword;

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
            RequestFocus(_email);
        }

        private async void Submit_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }
    }
}

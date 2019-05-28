using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class RegisterPage : BaseContentPage
    {
        private RegisterPageViewModel _vm;

        public RegisterPage()
        {
            InitializeComponent();
            _vm = BindingContext as RegisterPageViewModel;
            _vm.Page = this;
            MasterPasswordEntry = _masterPassword;
            ConfirmMasterPasswordEntry = _confirmMasterPassword;
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

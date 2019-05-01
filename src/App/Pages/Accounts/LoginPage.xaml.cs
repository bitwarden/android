using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPage : BaseContentPage
    {
        private LoginPageViewModel _vm;

        public LoginPage()
        {
            InitializeComponent();
            _vm = BindingContext as LoginPageViewModel;
            _vm.Page = this;
            MasterPasswordEntry = _masterPassword;
        }

        public Entry MasterPasswordEntry { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if(string.IsNullOrWhiteSpace(_vm.Email))
            {
                RequestFocus(_email);
            }
            else
            {
                RequestFocus(_masterPassword);
            }
        }

        private async void LogIn_Clicked(object sender, EventArgs e)
        {
            await _vm.LogInAsync();
        }
    }
}

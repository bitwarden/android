using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPage : BaseContentPage
    {
        private LoginPageViewModel _vm;

        public LoginPage(string email = null)
        {
            InitializeComponent();
            _vm = BindingContext as LoginPageViewModel;
            _vm.Page = this;
            _vm.Email = email;
            MasterPasswordEntry = _masterPassword;

            _email.ReturnType = ReturnType.Next;
            _email.ReturnCommand = new Command(() => _masterPassword.Focus());
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
            if(DoOnce())
            {
                await _vm.LogInAsync();
            }
        }

        private void Hint_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new HintPage()));
            }
        }
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class LoginPage : ContentPage
    {
        private LoginPageViewModel _vm;

        public LoginPage()
        {
            InitializeComponent();
            _vm = BindingContext as LoginPageViewModel;
            _vm.Page = this;
        }

        private async void LogIn_Clicked(object sender, EventArgs e)
        {
            await _vm.LogInAsync();
        }
    }
}

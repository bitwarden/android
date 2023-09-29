using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class LoginPasswordlessPage : BaseContentPage
    {
        private LoginPasswordlessViewModel _vm;

        public LoginPasswordlessPage(LoginPasswordlessDetails loginPasswordlessDetails)
        {
            InitializeComponent();
            _vm = BindingContext as LoginPasswordlessViewModel;
            _vm.Page = this;

            _vm.LoginRequest = loginPasswordlessDetails;

            ToolbarItems.Add(_closeItem);
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _vm.StartRequestTimeUpdater();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _vm.StopRequestTimeUpdater();
        }
    }
}

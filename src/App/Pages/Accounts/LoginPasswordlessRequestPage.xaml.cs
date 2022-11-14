using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPasswordlessRequestPage : BaseContentPage
    {
        private LoginPasswordlessRequestViewModel _vm;
        private readonly AppOptions _appOptions;

        public LoginPasswordlessRequestPage(string email, AppOptions appOptions = null)
        {
            InitializeComponent();
            _appOptions = appOptions;
            _vm = BindingContext as LoginPasswordlessRequestViewModel;
            _vm.Page = this;
            _vm.Email = email;
            _vm.StartTwoFactorAction = () => Device.BeginInvokeOnMainThread(async () => await StartTwoFactorAsync());
            _vm.LogInSuccessAction = () => Device.BeginInvokeOnMainThread(async () => await LogInSuccessAsync());
            _vm.UpdateTempPasswordAction = () => Device.BeginInvokeOnMainThread(async () => await UpdateTempPasswordAsync());
            _vm.CloseAction = () => { Navigation.PopModalAsync(); };

            _vm.CreatePasswordlessLoginCommand.Execute(null);
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _vm.StartCheckLoginRequestStatus();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _vm.StopCheckLoginRequestStatus();
        }

        private async Task StartTwoFactorAsync()
        {
            var page = new TwoFactorPage(false, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task LogInSuccessAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();
            Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
        }

        private async Task UpdateTempPasswordAsync()
        {
            var page = new UpdateTempPasswordPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}


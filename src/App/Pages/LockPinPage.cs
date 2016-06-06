using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using System.Collections.Generic;
using Bit.App.Models.Page;
using Bit.App.Controls;
using System.Diagnostics;

namespace Bit.App.Pages
{
    public class LockPinPage : ContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;

        public LockPinPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public PinPageModel Model { get; set; } = new PinPageModel();
        public PinControl PinControl { get; set; }

        public void Init()
        {
            PinControl = new PinControl(PinEntered);

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { PinControl.Label, PinControl.Entry, logoutButton }
            };

            Title = "Verify PIN";
            Content = stackLayout;
            BindingContext = Model;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            PinControl.Entry.Focus();
        }

        protected void PinEntered()
        {
            if(Model.PIN == "1234")
            {
                PinControl.Entry.Unfocus();
                Navigation.PopModalAsync();
            }
            else
            {
                _userDialogs.Alert("Invalid PIN. Try again.");
                Model.PIN = string.Empty;
                PinControl.Entry.Focus();
            }
        }

        private async Task LogoutAsync()
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _authService.LogOut();
            await Navigation.PopModalAsync();
            Application.Current.MainPage = new LoginNavigationPage();
        }
    }
}

using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.App.Models.Page;
using Bit.App.Controls;
using System.Linq;

namespace Bit.App.Pages
{
    public class LockPasswordPage : ExtendedContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly ICryptoService _cryptoService;

        public LockPasswordPage()
            : base(false)
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
            _cryptoService = Resolver.Resolve<ICryptoService>();

            Init();
        }

        public Entry Password { get; private set; }

        public void Init()
        {
            Password = new ExtendedEntry
            {
                HasBorder = false,
                IsPassword = true,
                Placeholder = AppResources.MasterPassword,
                ReturnType = Enums.ReturnType.Go
            };

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { Password, logoutButton }
            };

            var loginToolbarItem = new ToolbarItem("Submit", null, async () =>
            {
                await CheckPasswordAsync();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(loginToolbarItem);
            Title = "Verify Master Password";
            Content = stackLayout;
        }

        protected override bool OnBackButtonPressed()
        {
            return false;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            Password.Focus();
        }

        protected async Task CheckPasswordAsync()
        {
            if(string.IsNullOrWhiteSpace(Password.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword), AppResources.Ok);
                return;
            }

            var key = _cryptoService.MakeKeyFromPassword(Password.Text, _authService.Email);
            if(key.SequenceEqual(_cryptoService.Key))
            {
                await Navigation.PopModalAsync();
            }
            else
            {
                // TODO: keep track of invalid attempts and logout?

                _userDialogs.Alert("Invalid Master Password. Try again.");
                Password.Text = string.Empty;
                Password.Focus();
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
            Application.Current.MainPage = new HomePage();
        }
    }
}

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

namespace Bit.App.Pages
{
    public class LockPinPage : ExtendedContentPage
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
            PinControl = new PinControl();
            PinControl.OnPinEntered += PinEntered;
            PinControl.Label.SetBinding<PinPageModel>(Label.TextProperty, s => s.LabelText);
            PinControl.Entry.SetBinding<PinPageModel>(Entry.TextProperty, s => s.PIN);

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-default"]
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { PinControl.Label, logoutButton, PinControl.Entry }
            };

            var tgr = new TapGestureRecognizer();
            tgr.Tapped += Tgr_Tapped;

            Title = "Verify PIN";
            Content = stackLayout;
            Content.GestureRecognizers.Add(tgr);
            BindingContext = Model;
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            PinControl.Entry.Focus();
        }

        protected override bool OnBackButtonPressed()
        {
            return false;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            PinControl.Entry.Focus();
        }

        protected void PinEntered(object sender, EventArgs args)
        {
            if(Model.PIN == _authService.PIN)
            {
                PinControl.Entry.Unfocus();
                Navigation.PopModalAsync();
            }
            else
            {
                // TODO: keep track of invalid attempts and logout?

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
            Application.Current.MainPage = new HomePage();
        }
    }
}

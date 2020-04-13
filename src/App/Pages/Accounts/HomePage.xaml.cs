using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class HomePage : BaseContentPage
    {
        private readonly AppOptions _appOptions;
        private IMessagingService _messagingService;

        public HomePage(AppOptions appOptions = null)
        {
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _messagingService.Send("showStatusBar", false);
            _appOptions = appOptions;
            InitializeComponent();
            _logo.Source = !ThemeManager.UsingLightTheme ? "logo_white.png" : "logo.png";
        }

        public async Task DismissRegisterPageAndLogInAsync(string email)
        {
            await Navigation.PopModalAsync();
            await Navigation.PushModalAsync(new NavigationPage(new LoginPage(email, _appOptions)));
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _messagingService.Send("showStatusBar", false);
        }

        private void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new LoginPage(null, _appOptions)));
            }
        }

        private void Register_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new RegisterPage(this)));
            }
        }

        private void Settings_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new EnvironmentPage()));
            }
        }
    }
}

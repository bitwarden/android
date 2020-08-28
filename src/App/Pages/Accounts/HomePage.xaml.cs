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
        private readonly HomeViewModel _vm;
        private readonly AppOptions _appOptions;
        private IMessagingService _messagingService;

        public HomePage(AppOptions appOptions = null)
        {
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _messagingService.Send("showStatusBar", false);
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as HomeViewModel;
            _vm.Page = this;
            _vm.StartLoginAction = () => Device.BeginInvokeOnMainThread(async () => await StartLoginAsync());
            _vm.StartRegisterAction = () => Device.BeginInvokeOnMainThread(async () => await StartRegisterAsync());
            _vm.StartSsoLoginAction = () => Device.BeginInvokeOnMainThread(async () => await StartSsoLoginAsync());
            _vm.StartEnvironmentAction = () => Device.BeginInvokeOnMainThread(async () => await StartEnvironmentAsync());
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
        
        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartLoginAction();
            }
        }

        private async Task StartLoginAsync()
        {
            var page = new LoginPage(null, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void Register_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartRegisterAction();
            }
        }
        
        private async Task StartRegisterAsync()
        {
            var page = new RegisterPage(this);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void LogInSso_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartSsoLoginAction();
            }
        }

        private async Task StartSsoLoginAsync()
        {
            var page = new LoginSsoPage(_appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void Environment_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartEnvironmentAction();
            }
        }
        
        private async Task StartEnvironmentAsync()
        {
            var page = new EnvironmentPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}

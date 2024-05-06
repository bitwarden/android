using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class HomePage : BaseContentPage
    {
        private bool _checkRememberedEmail;
        private readonly HomeViewModel _vm;
        private readonly AppOptions _appOptions;
        private IBroadcasterService _broadcasterService;
        private IConditionedAwaiterManager _conditionedAwaiterManager;

        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        public HomePage(AppOptions appOptions = null)
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>();
            _conditionedAwaiterManager = ServiceContainer.Resolve<IConditionedAwaiterManager>();
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as HomeViewModel;
            _vm.Page = this;
            _vm.ShowCancelButton = _appOptions?.IosExtension ?? false;
            _vm.StartLoginAction = async () => await StartLoginAsync();
            _vm.StartRegisterAction = () => MainThread.BeginInvokeOnMainThread(async () => await StartRegisterAsync());
            _vm.StartSsoLoginAction = () => MainThread.BeginInvokeOnMainThread(async () => await StartSsoLoginAsync());
            _vm.StartEnvironmentAction = () => MainThread.BeginInvokeOnMainThread(async () => await StartEnvironmentAsync());
            _vm.CloseAction = async () =>
            {
                 await _accountListOverlay.HideAsync();
                await Navigation.PopModalAsync();
            };
            UpdateLogo();

            if (!_vm.ShowCancelButton)
            {
                ToolbarItems.Remove(_closeButton);
            }
            if (_appOptions?.HideAccountSwitcher ?? false)
            {
                 ToolbarItems.Remove(_accountAvatar);
            }
        }

        public bool PerformNavigationOnAccountChangedOnLoad { get; internal set; }

        void HomePage_Loaded(System.Object sender, System.EventArgs e)
        {
#if ANDROID
            // WORKAROUND: This is needed to fix the navigation when coming back from autofill when Accessibility Services is enabled
            // See App.xaml.cs -> CreateWindow(...) for more info.
            if (PerformNavigationOnAccountChangedOnLoad && ServiceContainer.TryResolve<IAccountsManager>(out var accountsManager))
            {
                PerformNavigationOnAccountChangedOnLoad = false;
                accountsManager.NavigateOnAccountChangeAsync().FireAndForget();
            }

            _conditionedAwaiterManager.SetAsCompleted(AwaiterPrecondition.AndroidWindowCreated);
#endif
        }

        public async Task DismissRegisterPageAndLogInAsync(string email)
        {
            await Navigation.PopModalAsync();
            await Navigation.PushModalAsync(new NavigationPage(new LoginPage(email, _appOptions)));
        }

        protected override async void OnNavigatedTo(NavigatedToEventArgs args)
        {
            base.OnNavigatedTo(args);

            await MainThread.InvokeOnMainThreadAsync(() => _mainContent.Content = _mainLayout);

            try
            {
                _accountAvatar?.OnAppearing();
                if (!_appOptions?.HideAccountSwitcher ?? false)
                {
                    await MainThread.InvokeOnMainThreadAsync(async () => _vm.AvatarImageSource = await GetAvatarImageSourceAsync(false));
                }
                _broadcasterService.Subscribe(nameof(HomePage), (message) =>
                {
                    if (message.Command is ThemeManager.UPDATED_THEME_MESSAGE_KEY)
                    {
                        MainThread.BeginInvokeOnMainThread(UpdateLogo);
                    }
                });

                await _vm.UpdateEnvironmentAsync();
            }
            catch (Exception ex)
            {
                _logger.Value?.Exception(ex);
            }
        }

        protected override void OnNavigatingFrom(NavigatingFromEventArgs args)
        {
            base.OnNavigatingFrom(args);

            _broadcasterService?.Unsubscribe(nameof(HomePage));
            _accountAvatar?.OnDisappearing();
        }

        protected override bool OnBackButtonPressed()
        {
            if (_accountListOverlay.IsVisible)
            {
                _accountListOverlay.HideAsync().FireAndForget();
                return true;
            }
            return false;
        }

        private void UpdateLogo()
        {
             _logo.Source = !ThemeManager.UsingLightTheme ? "logo_white.png" : "logo.png";
        }

        private void Cancel_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task StartLoginAsync()
        {
            var page = new LoginPage(_vm.Email, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
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

        private async Task StartEnvironmentAsync()
        {
            await _accountListOverlay.HideAsync();
            var page = new EnvironmentPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}

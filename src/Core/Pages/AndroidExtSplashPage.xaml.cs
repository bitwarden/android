using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Utilities.AccountManagement;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Core.Pages;

public partial class AndroidExtSplashPage : ContentPage
{
    private IConditionedAwaiterManager _conditionedAwaiterManager;
    private IVaultTimeoutService _vaultTimeoutService;
    private IStateService _stateService;
    private AppOptions _appOptions;

	public AndroidExtSplashPage(AppOptions appOptions)
	{
		InitializeComponent();
        _appOptions = appOptions ?? new AppOptions();
        _conditionedAwaiterManager = ServiceContainer.Resolve<IConditionedAwaiterManager>();
        _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
        _stateService = ServiceContainer.Resolve<IStateService>();
	}

    private async Task GetNavigationToExecuteAsync(AppOptions options, Window window)
    {
        await _conditionedAwaiterManager.GetAwaiterForPrecondition(AwaiterPrecondition.EnvironmentUrlsInited);

        var authed = await _stateService.IsAuthenticatedAsync();
        if (authed)
        {
            if (await _vaultTimeoutService.IsLoggedOutByTimeoutAsync() ||
                await _vaultTimeoutService.ShouldLogOutByTimeoutAsync())
            {
                // TODO implement orgIdentifier flow to SSO Login page, same as email flow below
                // var orgIdentifier = await _stateService.GetOrgIdentifierAsync();

                var email = await _stateService.GetEmailAsync();
                options.HideAccountSwitcher = await _stateService.GetActiveUserIdAsync() == null;
                var navParams = new LoginNavigationParams(email);
                if (navParams is LoginNavigationParams loginParams)
                {
                    window.Page = new NavigationPage(new LoginPage(loginParams.Email, options));
                }
            }
            else if (await _vaultTimeoutService.IsLockedAsync() ||
                     await _vaultTimeoutService.ShouldLockAsync())
            {
                /* //TODO: is lockParams needed here?
                if (navParams is LockNavigationParams lockParams)
                {
                    return () => new Window(new NavigationPage(new LockPage(Options, lockParams.AutoPromptBiometric)));
                }
                else
                {*/
                    window.Page = new NavigationPage(new LockPage(options));
                /*}*/
            }
            else if (options.FromAutofillFramework && options.SaveType.HasValue)
            {
                window.Page = new NavigationPage(new CipherAddEditPage(appOptions: options));
            }
            else if (options.Uri != null)
            {
                window.Page = new NavigationPage(new CipherSelectionPage(options));
            }
            else if (options.OtpData != null)
            {
                window.Page = new NavigationPage(new CipherSelectionPage(options));
            }
            else if (options.CreateSend != null)
            {
                window.Page = new NavigationPage(new SendAddEditPage(options));
            }
            else
            {
                window.Page = new TabsPage(options);
            }
        }
        else
        {
            options.HideAccountSwitcher = await _stateService.GetActiveUserIdAsync() == null;
            if (await _vaultTimeoutService.IsLoggedOutByTimeoutAsync() ||
                await _vaultTimeoutService.ShouldLogOutByTimeoutAsync())
            {
                // TODO implement orgIdentifier flow to SSO Login page, same as email flow below
                // var orgIdentifier = await _stateService.GetOrgIdentifierAsync();

                var email = await _stateService.GetEmailAsync();
                await _stateService.SetRememberedEmailAsync(email);

                window.Page = new NavigationPage(new HomePage(options));
            }
            else
            {
                window.Page = new NavigationPage(new HomePage(options));
            }
        }
    }

    private async void AndroidExtSplashPage_OnLoaded(object sender, EventArgs e)
    {
        await GetNavigationToExecuteAsync(_appOptions, this.Window);
    }
}

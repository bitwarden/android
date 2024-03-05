using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.Core.Pages;

public partial class AndroidNavigationRedirectPage : ContentPage
{
    private AppOptions _options;
	public AndroidNavigationRedirectPage(AppOptions options)
    {
        _options = options ?? new AppOptions();

		InitializeComponent();
	}

    private void AndroidNavigationRedirectPage_OnLoaded(object sender, EventArgs e)
    {
        if (ServiceContainer.TryResolve<IAccountsManager>(out var accountsManager))
        {
            accountsManager.NavigateOnAccountChangeAsync().FireAndForget();
        }
        else
        {
            Bit.App.App.MainPage = new NavigationPage(new HomePage(_options)); //Fallback scenario to load HomePage just in case something goes wrong when resolving IAccountsManager
        }

        if (ServiceContainer.TryResolve<IConditionedAwaiterManager>(out var conditionedAwaiterManager))
        {
            conditionedAwaiterManager?.SetAsCompleted(AwaiterPrecondition.AndroidWindowCreated);
        }
        else
        {
            LoggerHelper.LogEvenIfCantBeResolved(new InvalidOperationException("ConditionedAwaiterManager can't be resolved on Android Navigation redirection"));
        }
    }
}

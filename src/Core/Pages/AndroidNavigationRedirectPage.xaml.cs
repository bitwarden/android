using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Core.Pages;

public partial class AndroidNavigationRedirectPage : ContentPage
{
    private readonly IAccountsManager _accountsManager;
    private readonly IConditionedAwaiterManager _conditionedAwaiterManager;

	public AndroidNavigationRedirectPage()
    {
        _accountsManager = ServiceContainer.Resolve<IAccountsManager>("accountsManager");
        _conditionedAwaiterManager = ServiceContainer.Resolve<IConditionedAwaiterManager>();
		InitializeComponent();
	}

    private void AndroidNavigationRedirectPage_OnLoaded(object sender, EventArgs e)
    {
        _accountsManager.NavigateOnAccountChangeAsync().FireAndForget();
        _conditionedAwaiterManager.SetAsCompleted(AwaiterPrecondition.AndroidWindowCreated);
    }
}

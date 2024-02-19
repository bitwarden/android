using Bit.App.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Core.Pages;

public partial class AndroidNavigationRedirectPage : ContentPage
{
    private readonly IAccountsManager _accountsManager;

	public AndroidNavigationRedirectPage()
    {
        _accountsManager = ServiceContainer.Resolve<IAccountsManager>("accountsManager");
		InitializeComponent();
	}

    private void AndroidNavigationRedirectPage_OnLoaded(object sender, EventArgs e)
    {
        _accountsManager.NavigateOnAccountChangeAsync().FireAndForget();
    }
}

using Bit.App.Abstractions;

namespace Bit.App.Utilities.AccountManagement
{
    public class HomeNavigationParams : INavigationParams
    {
        public HomeNavigationParams(bool checkNavigateToLogin)
        {
            CheckNavigateToLogin = checkNavigateToLogin;
        }

        public bool CheckNavigateToLogin { get; }
    }
}

using Bit.App.Abstractions;

namespace Bit.App.Utilities.AccountManagement
{
    public class HomeNavigationParams : INavigationParams
    {
        public HomeNavigationParams(bool shouldCheckRememberEmail)
        {
            ShouldCheckRememberEmail = shouldCheckRememberEmail;
        }

        public bool ShouldCheckRememberEmail { get; }
    }
}

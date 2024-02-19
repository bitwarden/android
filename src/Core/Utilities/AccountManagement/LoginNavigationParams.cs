using Bit.App.Abstractions;

namespace Bit.App.Utilities.AccountManagement
{
    public class LoginNavigationParams : INavigationParams
    {
        public LoginNavigationParams(string email)
        {
            Email = email;
        }

        public string Email { get; }
    }
}

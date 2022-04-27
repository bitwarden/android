using Bit.UITests.Categories;
using Bit.UITests.Pages;
using Bit.UITests.Pages.Accounts;
using Bit.UITests.Setup;
using NUnit.Framework;
using Xamarin.UITest;

namespace Bit.UiTests.Tests
{
    public class LoginTests : BaseTestFixture
    {
        record AccountCredentials(string ServerUrl, string Email, string Password);

        private AccountCredentials[] _accounts =
        {
            new ("", "", ""),
            new ("", "", ""),
            new ("", "", ""),
        };

        public LoginTests(Platform platform)
            : base(platform)
        {
        }

        [Test]
        [SmokeTest]
        public void WaitForAppToLoad()
        {
            new HomePage();

            App.Screenshot("App loaded with success!");
        }

        //[Test]
        public void LoginWithSuccess()
        {
            Login(_accounts[0]);
            new TabsPage();
            App.Repl();

            App.Screenshot("After logging in with success, I can see the vault");
        }

        [Test]
        public void AccountSwitchWithSuccess()
        {
            Login(_accounts[0], true);
            new TabsPage()
                .TapAccountSwitchingAvatar()
                .TapAccountSwitchingAddAccount();

            Login(_accounts[1]);
            new TabsPage()
                .TapAccountSwitchingAvatar()
                .TapAccountSwitchingAddAccount();

            Login(_accounts[2]);

            new TabsPage()
                .TapAccountSwitchingAvatar();
        }

        private void Login(AccountCredentials account, bool changeEnvironment = false)
        {
            if (changeEnvironment)
            {
                new HomePage()
                    .TapEnvironmentAndNavigate();
                new EnvironmentPage()
                    .InputServerUrl(account.ServerUrl)
                    .TapSaveAndNavigate();
            }

            new HomePage()
                .TapLoginAndNavigate();

            new LoginPage()
                .InputEmail(account.Email)
                .InputPassword(account.Password)
                .TapLoginAndNavigate();
        }
    }
}

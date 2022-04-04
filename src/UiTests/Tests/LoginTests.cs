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
        private const string _testServerUrl = "zamboni";
        private const string _email = "zamboni";
        private const string _password = "zamboni";

        public LoginTests(Platform platform)
            : base(platform)
        {
        }

        //[Test] 
        public void OpenREPL()
        {
            App.Repl();
        }

        [Test]
        [SmokeTest]
        public void WaitForAppToLoad()
        {
            new HomePage();

            App.Screenshot("App loaded with success!");
        }

        [Test]
        public void LoginWithSuccess()
        {

            new HomePage()
                .TapEnvironmentAndNavigate();

            new EnvironmentPage()
                .InputServerUrl(_testServerUrl)
                .TapSaveAndNavigate();

            new HomePage()
                .TapLoginAndNavigate();

            new LoginPage()
                .InputEmail(_email)
                .InputPassword(_password)
                .TapLoginAndNavigate();

            new TabsPage();

            App.Screenshot("After logging in with success, I can see the vault");
        }
    }
}

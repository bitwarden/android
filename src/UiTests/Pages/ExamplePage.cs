using System;
using Bit.UITests.Setup;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Pages
{
    public class ExamplePage : BasePage
    {
        private readonly Query _loginButton;
        private readonly Query _passwordInput;

        public ExamplePage()
            : base()
        {
            if (OnAndroid)
            {
                _loginButton = x => x.Marked("loginpage_login_button");
                _passwordInput = x => x.Marked("password_input");

                return;
            }

            if (OniOS)
            {
                _loginButton = x => x.Marked("loginpage_login_button");
                _passwordInput = x => x.Marked("password_input");
            }
        }

        protected override PlatformQuery Trait => new PlatformQuery
        {
            Android = x => x.Marked("password_input"),
            iOS = x => x.Marked("password_input"),
        };

        public ExamplePage TapLogin()
        {
            App.Tap(_loginButton);
            return this;
        }

        public ExamplePage InputPassword(string password)
        {
            App.Tap(_passwordInput);
            App.EnterText(password);
            App.DismissKeyboard();

            App.Screenshot("After I input the email and password fields, I can see both fields filled");

            return this;
        }

    }
}

using System;
using Bit.UITests.Extensions;
using Bit.UITests.Setup;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Pages.Accounts
{
    public class LoginPage : BasePage
    {
        private readonly Query _loginButton;
        private readonly Query _cancelButton;
        private readonly Query _passwordVisibilityToggle;

        private readonly Query _emailInput;
        private readonly Query _passwordInput;


        public LoginPage()
            : base()
        {
            if (OnAndroid)
            {
                _loginButton = x => x.Marked("loginpage_login_button");
                _cancelButton = x => x.Marked("cancel_button");

                //TODO a11y uses the same fields as the UI tests and we're prioritising that
                // improve this by getting the app runtime locale and use the i18n service here instead
                _passwordVisibilityToggle = x => x.Marked("Toggle Visibility");

                _emailInput = x => x.Marked("email_input");
                _passwordInput = x => x.Marked("password_input");

                return;
            }

            if (OniOS)
            {
                _loginButton = x => x.Marked("loginpage_login_button");
                _cancelButton = x => x.Marked("cancel_button");
                _passwordVisibilityToggle = x => x.Marked("Toggle Visibility");

                _emailInput = x => x.Marked("email_input");
                _passwordInput = x => x.Marked("password_input");
            }
        }

        protected override PlatformQuery Trait => new PlatformQuery
        {
            Android = x => x.Marked("email_input"),
            iOS = x => x.Marked("email_input"),
        };

        public LoginPage TapLoginAndNavigate()
        {
            App.Tap(_loginButton);
            return this;
        }

        public LoginPage TapCancelAndNavigate()
        {
            App.Tap(_cancelButton);
            return this;
        }

        public LoginPage TapPasswordVisibilityToggle()
        {
            App.Tap(_passwordVisibilityToggle);
            return this;
        }

        public LoginPage InputEmail(string email)
        {
            App.ClearText(_emailInput);
            App.EnterText(_emailInput, email);
            App.DismissKeyboard();
            return this;
        }

        public LoginPage InputPassword(string password)
        {
            App.Tap(_passwordInput);
            App.EnterText(password);
            App.DismissKeyboard();            
            App.WaitAndScreenshot("After I input the email and password fields, I can see both fields filled");

            return this;
        }

    }
}

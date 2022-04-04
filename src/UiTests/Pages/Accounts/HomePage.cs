using Bit.UITests.Setup;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Pages.Accounts
{
    public class HomePage : BasePage
    {
        private readonly Query _loginButton;
        private readonly Query _environmentButton;

        public HomePage()
            : base()
        {
            if (OnAndroid)
            {
                _loginButton = x => x.Marked("homepage_login_button");

                //TODO a11y uses the same fields as the UI tests and we're prioritising that
                // improve this by getting the app runtime locale and use the i18n service here instead
                _environmentButton = x => x.Marked("Options");
                //_environmentButton = x => x.Marked("environment_button");
                return;
            }

            if (OniOS)
            {
                _loginButton = x => x.Marked("homepage_login_button");
                _environmentButton = x => x.Marked("Options");
            }
        }

        protected override PlatformQuery Trait => new PlatformQuery
        {
            Android = x => x.Marked("logo_image"),
            iOS = x => x.Marked("logo_image"),
        };

        public HomePage TapLoginAndNavigate()
        {
            App.Tap(_loginButton);
            return this;
        }

        public HomePage TapEnvironmentAndNavigate()
        {
            App.Tap(_environmentButton);
            return this;
        }
    }
}

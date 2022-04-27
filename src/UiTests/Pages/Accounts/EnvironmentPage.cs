using Bit.UITests.Extensions;
using Bit.UITests.Setup;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Pages.Accounts
{
    public class EnvironmentPage : BasePage
    {
        private readonly Query _saveButton;
        private readonly Query _serverUrlInput;

        public EnvironmentPage()
            : base()
        {
            if (OnAndroid)
            {
                _saveButton = x => x.Marked("save_button");
                _serverUrlInput = x => x.Marked("server_input");

                return;
            }

            if (OniOS)
            {
                _saveButton = x => x.Marked("save_button");
                _serverUrlInput = x => x.Marked("server_input");
            }
        }

        protected override PlatformQuery Trait => new PlatformQuery
        {
            Android = x => x.Marked("server_input"),
            iOS = x => x.Marked("server_input"),
        };

        public EnvironmentPage TapSaveAndNavigate()
        {
            App.Tap(_saveButton);
            WaitForPageToLeave();
            return this;
        }

        public EnvironmentPage InputServerUrl(string serverUrl)
        {
            App.ClearText(_serverUrlInput);
            App.EnterText(_serverUrlInput, serverUrl);
            App.DismissKeyboard();
            App.WaitAndScreenshot("After inserting the server url, I can see the field filled");
            return this;
        }
    }
}

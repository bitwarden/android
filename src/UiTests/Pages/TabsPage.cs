using System;
using Bit.UITests.Extensions;
using Bit.UITests.Helpers;
using Bit.UITests.Setup;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Pages
{
    public class TabsPage : BasePage
    {
        private readonly Query _vaultTab;
        private readonly Query _sendTab;
        private readonly Query _accountSwitchingAvatar;
        private readonly Query _accountSwitchingAddAccount;


        public TabsPage()
            : base()
        {
            _vaultTab = x => x.Marked("My Vault");
            _sendTab = x => x.Marked("Send");
            _accountSwitchingAvatar = x => x.Marked("Account");
            _accountSwitchingAddAccount = x => x.Marked("Add Account");

            WaitForNoLoader();
        }

        protected override PlatformQuery Trait => new PlatformQuery
        {
            Android = x => x.Marked("Send"),
            iOS = x => x.Marked("Send"),
        };

        public TabsPage WaitForNoLoader()
        {
            App.WaitForNoElement(LoadingIndicator, timeout: CustomWaitTimes.DefaultCustomTimeout);
            App.WaitAndScreenshot("Page finished loading");
            return this;
        }

        public TabsPage TapAccountSwitchingAvatar()
        {
            App.WaitForElement(_accountSwitchingAvatar);
            App.Tap(_accountSwitchingAvatar);
            App.WaitAndScreenshot("Tapping the avatar, I can see the account switching panel");

            return this;
        }

        public TabsPage TapAccountSwitchingAddAccount()
        {
            App.Tap(_accountSwitchingAddAccount);
            return this;
        }

        public TabsPage TapVaultTab()
        {
            App.Tap(_vaultTab);
            App.WaitAndScreenshot("Tapping the Vault tab, I can see the Vault view");
            return this;
        }

        public TabsPage TapSTab()
        {
            App.Tap(_sendTab);
            App.WaitAndScreenshot("Tapping the Send tab, I can see the Send view");
            return this;
        }
    }
}

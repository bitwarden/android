using System;
using Bit.UITests.Setup;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Pages
{
    public class TabsPage : BasePage
    {
        private readonly Query _vaultTab;
        private readonly Query _sendTab;

        public TabsPage()
            : base()
        {

            _vaultTab = x => x.Marked("My Vault");
            _sendTab = x => x.Marked("Send");
        }

        protected override PlatformQuery Trait => new PlatformQuery
        {
            Android = x => x.Marked("Send"),
            iOS = x => x.Marked("Send"),
        };
    }
}

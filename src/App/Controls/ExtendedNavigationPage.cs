using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedNavigationPage : NavigationPage
    {
        public ExtendedNavigationPage()
            : base()
        {
            SetDefaults();
        }

        public ExtendedNavigationPage(Page root)
            : base(root)
        {
            SetDefaults();
        }

        private void SetDefaults()
        {
            // default colors for our app
            BarBackgroundColor = Color.FromHex("3c8dbc");
            BarTextColor = Color.White;
        }
    }
}

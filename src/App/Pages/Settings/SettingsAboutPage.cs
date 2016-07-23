using System;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsAboutPage : ExtendedContentPage
    {
        public SettingsAboutPage()
        {
            Init();
        }

        public void Init()
        {
            // TODO: version, credits, etc

            var stackLayout = new StackLayout { };

            Title = "About bitwarden";
            Content = stackLayout;
        }
    }
}

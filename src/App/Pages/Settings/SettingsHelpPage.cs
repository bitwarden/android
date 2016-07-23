using System;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsHelpPage : ExtendedContentPage
    {
        public SettingsHelpPage()
        {
            Init();
        }

        public void Init()
        {
            // TODO: hockeyapp feedback, link to website help

            var stackLayout = new StackLayout { };

            Title = "Help and Support";
            Content = stackLayout;
        }
    }
}

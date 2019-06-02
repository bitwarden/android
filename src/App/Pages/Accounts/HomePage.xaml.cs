using Bit.App.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class HomePage : BaseContentPage
    {
        public HomePage()
        {
            InitializeComponent();
            var theme = ThemeManager.GetTheme();
            _logo.Source = theme == "dark" || theme == "black" ? "logo_white.png" : "logo.png";
        }

        private void LogIn_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new LoginPage()));
            }
        }

        private void Register_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new RegisterPage()));
            }
        }

        private void Settings_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new EnvironmentPage()));
            }
        }
    }
}

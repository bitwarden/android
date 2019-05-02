using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class HomePage : ContentPage
    {
        public HomePage()
        {
            InitializeComponent();
        }

        private void LogIn_Clicked(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new NavigationPage(new LoginPage()));
        }

        private void Register_Clicked(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new NavigationPage(new RegisterPage()));
        }

        private void Settings_Clicked(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new NavigationPage(new EnvironmentPage()));
        }
    }
}

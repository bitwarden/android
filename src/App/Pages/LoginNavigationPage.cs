using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginNavigationPage : NavigationPage
    {
        public LoginNavigationPage()
            : base(new LoginPage())
        {
            BarBackgroundColor = Color.FromHex("3c8dbc");
            BarTextColor = Color.FromHex("ffffff");
            Title = AppResources.LogInNoun;
        }
    }
}

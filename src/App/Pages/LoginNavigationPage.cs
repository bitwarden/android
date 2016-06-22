using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginNavigationPage : NavigationPage
    {
        public LoginNavigationPage()
            : base(new LoginPage())
        {
            BarBackgroundColor = Color.Transparent;
            BarTextColor = Color.FromHex("333333");
            Title = AppResources.LogInNoun;
        }
    }
}

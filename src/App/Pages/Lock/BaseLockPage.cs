using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class BaseLockPage : ExtendedContentPage
    {
        public BaseLockPage()
            : base(false, false)
        {

            UserDialogs = Resolver.Resolve<IUserDialogs>();
        }

        protected IUserDialogs UserDialogs { get; set; }

        protected override bool OnBackButtonPressed()
        {
            if(Device.RuntimePlatform == Device.Android)
            {
                MessagingCenter.Send(Application.Current, "BackgroundApp");
            }

            return true;
        }

        protected async Task LogoutAsync()
        {
            if(!await UserDialogs.ConfirmAsync(AppResources.LogoutConfirmation, null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }
    }
}

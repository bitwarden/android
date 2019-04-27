using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

[assembly: XamlCompilation(XamlCompilationOptions.Compile)]
namespace Bit.App
{
    public partial class App : Application
    {
        private readonly MobileI18nService _i18nService;
        private readonly IUserService _userService;
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;

        public App()
        {
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService;

            InitializeComponent();
            SetCulture();
            ThemeManager.SetThemeStyle("light");
            MainPage = new HomePage();
            var mainPageTask = SetMainPageAsync();

            ServiceContainer.Resolve<MobilePlatformUtilsService>("platformUtilsService").Init();
            _broadcasterService.Subscribe(nameof(App), async (message) =>
            {
                if(message.Command == "showDialog")
                {
                    var details = message.Data as DialogDetails;
                    var confirmed = true;
                    var confirmText = string.IsNullOrWhiteSpace(details.ConfirmText) ?
                        AppResources.Ok : details.ConfirmText;
                    if(!string.IsNullOrWhiteSpace(details.CancelText))
                    {
                        confirmed = await MainPage.DisplayAlert(details.Title, details.Text, confirmText,
                            details.CancelText);
                    }
                    else
                    {
                        await MainPage.DisplayAlert(details.Title, details.Text, details.ConfirmText);
                    }
                    _messagingService.Send("showDialogResolve", new Tuple<int, bool>(details.DialogId, confirmed));
                }
            });
        }

        protected override void OnStart()
        {
            // Handle when your app starts
        }

        protected override void OnSleep()
        {
            // Handle when your app sleeps
        }

        protected override void OnResume()
        {
            // Handle when your app resumes
        }

        private void SetCulture()
        {
            // Calendars are removed by linker. ref https://bugzilla.xamarin.com/show_bug.cgi?id=59077
            new System.Globalization.ThaiBuddhistCalendar();
            new System.Globalization.HijriCalendar();
            new System.Globalization.UmAlQuraCalendar();
        }

        private async Task SetMainPageAsync()
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if(authed)
            {
                Current.MainPage = new TabsPage();
            }
            else
            {
                Current.MainPage = new HomePage();
            }
        }
    }
}

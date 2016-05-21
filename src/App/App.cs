using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Xamarin.Forms;
using System.Diagnostics;
using Plugin.Fingerprint.Abstractions;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;

namespace Bit.App
{
    public class App : Application
    {
        private readonly IDatabaseService _databaseService;
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;

        public App(
            IAuthService authService,
            IDatabaseService databaseService,
            IFingerprint fingerprint,
            ISettings settings)
        {
            _databaseService = databaseService;
            _authService = authService;
            _fingerprint = fingerprint;
            _settings = settings;

            if(authService.IsAuthenticated)
            {
                MainPage = new MainPage();
            }
            else
            {
                MainPage = new LoginNavigationPage();
            }

            MainPage.BackgroundColor = Color.FromHex("ecf0f5");

            MessagingCenter.Subscribe<App>(this, "Lock", async (sender) =>
            {
                await CheckLockAsync();
            });
        }

        protected override void OnStart()
        {
            // Handle when your app starts
            CheckLockAsync();
            _databaseService.CreateTables();

            Debug.WriteLine("OnStart");
        }

        protected override void OnSleep()
        {
            // Handle when your app sleeps
            Debug.WriteLine("OnSleep");
        }

        protected override void OnResume()
        {
            // Handle when your app resumes
            Debug.WriteLine("OnResume");
        }

        private async Task CheckLockAsync()
        {
            if(_authService.IsAuthenticated && Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockFingerprintPage == null)
            {
                await Current.MainPage.Navigation.PushModalAsync(new LockFingerprintPage(), false);
            }
        }
    }
}

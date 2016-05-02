using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Xamarin.Forms;

namespace Bit.App
{
    public class App : Application
    {
        private readonly IDatabaseService _databaseService;
        private readonly IAuthService _authService;

        public App(IAuthService authService, IDatabaseService databaseService)
        {
            _databaseService = databaseService;
            _authService = authService;

            if(authService.IsAuthenticated)
            {
                MainPage = new MainPage();
            }
            else
            {
                MainPage = new LoginNavigationPage();
            }

            MainPage.BackgroundColor = Color.FromHex("ecf0f5");
        }

        protected override void OnStart()
        {
            // Handle when your app starts
            _databaseService.CreateTables();
        }

        protected override void OnSleep()
        {
            // Handle when your app sleeps
        }

        protected override void OnResume()
        {
            // Handle when your app resumes
        }
    }
}

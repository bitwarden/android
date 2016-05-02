using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Views;
using Xamarin.Forms;

namespace Bit.App
{
    public class App : Application
    {
        private readonly IDatabaseService _databaseService;

        public App(IAuthService authService, IDatabaseService databaseService)
        {
            _databaseService = databaseService;

            if(authService.IsAuthenticated)
            {
                var nav = new NavigationPage(new VaultListPage());
                nav.BarBackgroundColor = Color.FromHex("3c8dbc");
                nav.BarTextColor = Color.FromHex("ffffff");

                MainPage = nav;
            }
            else
            {
                var nav = new NavigationPage(new LoginPage());
                nav.BarBackgroundColor = Color.FromHex("3c8dbc");
                nav.BarTextColor = Color.FromHex("ffffff");

                MainPage = nav;
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

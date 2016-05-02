using System;
using System.Collections.Generic;
using Bit.App.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SettingsPage : ContentPage
    {
        private ListView _listView = new ListView();

        public SettingsPage()
        {
            var authService = Resolver.Resolve<IAuthService>();

            var logoutButton = new Button
            {
                Text = "Log Out",
                Command = new Command(() =>
                {
                    authService.LogOut();
                    Application.Current.MainPage = new LoginNavigationPage();
                })
            };

            var stackLayout = new StackLayout { };
            stackLayout.Children.Add(logoutButton);

            Title = "Settings";
            Content = stackLayout;
        }
    }
}

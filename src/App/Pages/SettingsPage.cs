using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SettingsPage : ContentPage
    {
        private readonly IAuthService _authService;

        public SettingsPage()
        {
            _authService = Resolver.Resolve<IAuthService>();

            Init();
        }

        private void Init()
        {
            var foldersCell = new TextCell { Text = "Folders" };
            foldersCell.Tapped += FoldersCell_Tapped;

            var table = new TableView
            {
                Intent = TableIntent.Menu,
                Root = new TableRoot
                {
                    new TableSection("Manage Folders")
                    {
                        foldersCell
                    }
                }
            };

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(() =>
                {
                    _authService.LogOut();
                    Application.Current.MainPage = new LoginNavigationPage();
                })
            };

            var stackLayout = new StackLayout { };
            stackLayout.Children.Add(table);
            stackLayout.Children.Add(logoutButton);

            Title = AppResources.Settings;
            Content = stackLayout;
        }

        private void FoldersCell_Tapped(object sender, EventArgs e)
        {

        }
    }
}

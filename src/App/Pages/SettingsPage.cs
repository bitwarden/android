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
            var foldersLayout = new StackLayout { Orientation = StackOrientation.Horizontal };
            foldersLayout.Children.Add(new Label
            {
                Text = "Folders",
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                Margin = new Thickness(10, 0, 0, 0)
            });
            foldersLayout.Children.Add(new Image
            {
                Source = ImageSource.FromFile("ion-chevron-right.png"),
                Opacity = 0.3,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, 0, 10, 0)
            });
            var foldersCell = new ViewCell { View = foldersLayout };
            foldersCell.Tapped += FoldersCell_Tapped;

            var table = new TableView
            {
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

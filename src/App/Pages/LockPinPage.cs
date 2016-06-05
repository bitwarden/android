using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using System.Collections.Generic;
using Bit.App.Models.Page;

namespace Bit.App.Pages
{
    public class LockPinPage : ContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;

        public LockPinPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public PinPageModel Model { get; set; } = new PinPageModel();

        public void Init()
        {
            var label = new Label
            {
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = 40,
                TextColor = Color.FromHex("333333")
            };
            label.SetBinding<PinPageModel>(Label.TextProperty, s => s.LabelText);

            var grid = new Grid();
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });

            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            Action updateLabelAction = new Action(UpdateLabel);
            grid.Children.Add(new PinButton("1", this, updateLabelAction), 0, 0);
            grid.Children.Add(new PinButton("2", this, updateLabelAction), 1, 0);
            grid.Children.Add(new PinButton("3", this, updateLabelAction), 2, 0);

            grid.Children.Add(new PinButton("4", this, updateLabelAction), 0, 1);
            grid.Children.Add(new PinButton("5", this, updateLabelAction), 1, 1);
            grid.Children.Add(new PinButton("6", this, updateLabelAction), 2, 1);

            grid.Children.Add(new PinButton("7", this, updateLabelAction), 0, 2);
            grid.Children.Add(new PinButton("8", this, updateLabelAction), 1, 2);
            grid.Children.Add(new PinButton("9", this, updateLabelAction), 2, 2);

            grid.Children.Add(new Label(), 0, 3);
            grid.Children.Add(new PinButton("0", this, updateLabelAction), 1, 3);
            grid.Children.Add(new DeleteButton(this, updateLabelAction), 2, 3);

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 10,
                Children = { label, grid, logoutButton }
            };

            Title = "Verify PIN";
            Content = stackLayout;
            BindingContext = Model;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
        }

        private async Task LogoutAsync()
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _authService.LogOut();
            await Navigation.PopModalAsync();
            Application.Current.MainPage = new LoginNavigationPage();
        }

        private void UpdateLabel()
        {
            var newText = string.Empty;
            for(int i = 0; i < 4; i++)
            {
                if(Model.PIN.Count <= i)
                {
                    newText += "_  ";
                }
                else
                {
                    newText += "*  ";
                }
            }

            Model.LabelText = newText.TrimEnd();
        }

        public class PinButton : Button
        {
            public PinButton(string text, LockPinPage page, Action updateLabelAction)
            {
                Text = text;
                Command = new Command(() =>
                {
                    if(page.Model.PIN.Count >= 4)
                    {
                        return;
                    }

                    page.Model.PIN.Add(text);
                    updateLabelAction();
                });
                CommandParameter = text;
            }
        }

        public class DeleteButton : Button
        {
            public DeleteButton(LockPinPage page, Action updateLabelAction)
            {
                Text = "Delete";
                Command = new Command(() =>
                {
                    if(page.Model.PIN.Count == 0)
                    {
                        return;
                    }

                    page.Model.PIN.RemoveAt(page.Model.PIN.Count - 1);
                    updateLabelAction();
                });
            }
        }
    }
}

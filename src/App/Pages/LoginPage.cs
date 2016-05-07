using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Behaviors;
using Bit.App.Models.Api;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class LoginPage : ContentPage
    {
        public LoginPage()
        {
            var cryptoService = Resolver.Resolve<ICryptoService>();
            var authService = Resolver.Resolve<IAuthService>();

            var emailEntry = new Entry
            {
                Keyboard = Keyboard.Email,
                Placeholder = AppResources.EmailAddress
            };

            emailEntry.Behaviors.Add(new RequiredValidationBehavior());
            emailEntry.Behaviors.Add(new EmailValidationBehavior());

            var masterPasswordEntry = new Entry
            {
                IsPassword = true,
                Placeholder = AppResources.MasterPassword
            };

            masterPasswordEntry.Behaviors.Add(new RequiredValidationBehavior());

            var loginButton = new Button
            {
                Text = AppResources.LogIn,
                Command = new Command(async () =>
                {
                    if(string.IsNullOrWhiteSpace(emailEntry.Text))
                    {
                        await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress), AppResources.Ok);
                        return;
                    }

                    if(string.IsNullOrWhiteSpace(masterPasswordEntry.Text))
                    {
                        await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword), AppResources.Ok);
                        return;
                    }

                    var key = cryptoService.MakeKeyFromPassword(masterPasswordEntry.Text, emailEntry.Text);

                    var request = new TokenRequest
                    {
                        Email = emailEntry.Text,
                        MasterPasswordHash = cryptoService.HashPasswordBase64(key, masterPasswordEntry.Text)
                    };

                    var response = await authService.TokenPostAsync(request);
                    if(!response.Succeeded)
                    {
                        await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.First().Message, AppResources.Ok);
                        return;
                    }

                    cryptoService.Key = key;
                    authService.Token = response.Result.Token;
                    authService.UserId = response.Result.Profile.Id;

                    Application.Current.MainPage = new MainPage();
                })
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(10, 50, 10, 0)
            };
            stackLayout.Children.Add(emailEntry);
            stackLayout.Children.Add(masterPasswordEntry);
            stackLayout.Children.Add(loginButton);

            Title = AppResources.LogIn;
            Content = stackLayout;
            NavigationPage.SetHasNavigationBar(this, false);
        }
    }
}

using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Utilities;
using System;
using System.Reflection;
using Xamarin.Forms;
using Xamarin.Forms.StyleSheets;
using Xamarin.Forms.Xaml;

[assembly: XamlCompilation(XamlCompilationOptions.Compile)]
namespace Bit.App
{
    public partial class App : Application
    {
        public App()
        {
            InitializeComponent();

            ThemeManager.SetTheme("light");
            MainPage = new TabsPage();

            MessagingCenter.Subscribe<Application, DialogDetails>(Current, "ShowDialog", async (sender, details) =>
            {
                var confirmed = true;
                // TODO: ok text
                var confirmText = string.IsNullOrWhiteSpace(details.ConfirmText) ? "Ok" : details.ConfirmText;
                if(!string.IsNullOrWhiteSpace(details.CancelText))
                {
                    confirmed = await MainPage.DisplayAlert(details.Title, details.Text, confirmText,
                        details.CancelText);
                }
                else
                {
                    await MainPage.DisplayAlert(details.Title, details.Text, details.ConfirmText);
                }
                MessagingCenter.Send(Current, "ShowDialogResolve", new Tuple<int, bool>(details.DialogId, confirmed));
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
    }
}

using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Abstractions;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SettingsAboutPage : ExtendedContentPage
    {
        private readonly IAppInfoService _appInfoService;

        public SettingsAboutPage()
        {
            _appInfoService = Resolver.Resolve<IAppInfoService>();
            Init();
        }

        public void Init()
        {
            var logo = new Image
            {
                Source = "logo",
                HorizontalOptions = LayoutOptions.Center
            };

            var versionLabel = new Label
            {
                Text = $@"Version {_appInfoService.Version}
© 8bit Solutions LLC 2015-{DateTime.Now.Year}",
                HorizontalTextAlignment = TextAlignment.Center
            };

            var creditsButton = new Button
            {
                Text = "Credits",
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                Margin = new Thickness(15, 0, 15, 25),
                Command = new Command(async () => await Navigation.PushAsync(new SettingsCreditsPage())),
                HorizontalOptions = LayoutOptions.Center
            };

            var stackLayout = new StackLayout
            {
                Children = { logo, versionLabel, creditsButton },
                VerticalOptions = LayoutOptions.Center,
                Spacing = 20,
                Margin = new Thickness(0, 0, 0, 40)
            };

            Title = "About bitwarden";
            Content = new ScrollView { Content = stackLayout };
            NavigationPage.SetBackButtonTitle(this, "About");
        }
    }
}

using System;
using Bit.App.Controls;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;
using Bit.App.Resources;

namespace Bit.App.Pages
{
    public class ToolsAutofillServicePage : ExtendedContentPage
    {
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public ToolsAutofillServicePage()
        {
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public void Init()
        {
            var serviceLabel = new Label
            {
                Text = AppResources.AutofillDescription,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            };

            var comingSoonLabel = new Label
            {
                Text = AppResources.ComingSoon,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label)),
                TextColor = Color.Black
            };

            var progressButton = new ExtendedButton
            {
                Text = AppResources.SeeDevProgress,
                Command = new Command(() =>
                {
                    _googleAnalyticsService.TrackAppEvent("SeeAutofillProgress");
                    Device.OpenUri(new Uri("https://github.com/bitwarden/mobile/issues/1"));
                }),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"],
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Button))
            };

            var stackLayout = new StackLayout
            {
                Children = { serviceLabel, comingSoonLabel, progressButton },
                Orientation = StackOrientation.Vertical,
                Spacing = 10,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            Title = AppResources.AutofillService;
            Content = new ScrollView { Content = stackLayout };
        }
    }
}

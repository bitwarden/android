using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;
using Bit.App.Resources;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class ToolsExtensionPage : ExtendedContentPage
    {
        private readonly ISettings _settings;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public ToolsExtensionPage()
        {
            _settings = Resolver.Resolve<ISettings>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            Model = new AppExtensionPageModel(_settings);

            Init();
        }

        public AppExtensionPageModel Model { get; private set; }

        public void Init()
        {
            // Not Started

            var notStartedLabel = new Label
            {
                Text = AppResources.ExtensionInstantAccess,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var notStartedSublabel = new Label
            {
                Text = AppResources.ExtensionTurnOn,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var notStartedImage = new CachedImage
            {
                Source = "ext-more",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0),
                WidthRequest = 290,
                HeightRequest = 252
            };

            var notStartedButton = new ExtendedButton
            {
                Text = AppResources.ExtensionEnable,
                Command = new Command(() => ShowExtension("NotStartedEnable")),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var notStartedStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 20, 20, 30),
                Children = { notStartedLabel, notStartedSublabel, notStartedImage, notStartedButton },
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            notStartedStackLayout.SetBinding(IsVisibleProperty, nameof(AppExtensionPageModel.NotStarted));

            // Not Activated 

            var notActivatedLabel = new Label
            {
                Text = AppResources.ExtensionAlmostDone,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var notActivatedSublabel = new Label
            {
                Text = AppResources.ExtensionTapIcon,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var notActivatedImage = new CachedImage
            {
                Source = "ext-act",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0),
                WidthRequest = 290,
                HeightRequest = 252
            };

            var notActivatedButton = new ExtendedButton
            {
                Text = AppResources.ExtensionEnable,
                Command = new Command(() => ShowExtension("NotActivatedEnable")),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var notActivatedStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 20, 20, 30),
                Children = { notActivatedLabel, notActivatedSublabel, notActivatedImage, notActivatedButton },
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            notActivatedStackLayout.SetBinding(IsVisibleProperty, nameof(AppExtensionPageModel.StartedAndNotActivated));

            // Activated 

            var activatedLabel = new Label
            {
                Text = AppResources.ExtensionReady,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var activatedSublabel = new Label
            {
                Text = AppResources.ExtensionInSafari,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                Margin = new Thickness(0, 10, 0, 0)
            };

            var activatedImage = new CachedImage
            {
                Source = "ext-use",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0),
                WidthRequest = 290,
                HeightRequest = 252
            };

            var activatedButton = new ExtendedButton
            {
                Text = AppResources.ExtensionSeeApps,
                Command = new Command(() =>
                {
                    _googleAnalyticsService.TrackAppEvent("SeeSupportedApps");
                    Device.OpenUri(new Uri("https://bitwarden.com/ios/"));
                }),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var activatedButtonReenable = new ExtendedButton
            {
                Text = AppResources.ExntesionReenable,
                Command = new Command(() => ShowExtension("Re-enable")),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            var activatedStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 10,
                Padding = new Thickness(20, 20, 20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand,
                Children = { activatedLabel, activatedSublabel, activatedImage, activatedButton, activatedButtonReenable }
            };

            activatedStackLayout.SetBinding(IsVisibleProperty, nameof(AppExtensionPageModel.StartedAndActivated));

            var stackLayout = new StackLayout
            {
                Children = { notStartedStackLayout, notActivatedStackLayout, activatedStackLayout },
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.AppExtension;
            Content = new ScrollView { Content = stackLayout };
            BindingContext = Model;
        }

        private void ShowExtension(string type)
        {
            _googleAnalyticsService.TrackAppEvent("ShowExtension", type);
            MessagingCenter.Send(Application.Current, "ShowAppExtension", this);
        }

        public void EnabledExtension(bool enabled)
        {
            _googleAnalyticsService.TrackAppEvent("EnabledExtension", enabled.ToString());
            Model.Started = true;
            if(!Model.Activated && enabled)
            {
                Model.Activated = enabled;
            }
        }
    }
}

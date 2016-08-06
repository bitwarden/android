using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;

namespace Bit.App.Pages
{
    public class ToolsExtensionPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public ToolsExtensionPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
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
                Text = "Get instant access to your passwords!",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var notStartedSublabel = new Label
            {
                Text = "To turn on bitwarden in Safari and other apps, tap the \"more\" icon on the bottom row of the menu.",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var notStartedImage = new Image
            {
                Source = "ext-more",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0)
            };

            var notStartedButton = new Button
            {
                Text = "Enable App Extension",
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

            notStartedStackLayout.SetBinding<AppExtensionPageModel>(IsVisibleProperty, m => m.NotStarted);

            // Not Activated 

            var notActivatedLabel = new Label
            {
                Text = "Almost done!",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var notActivatedSublabel = new Label
            {
                Text = "Tap the bitwarden icon in the menu to launch the extension.",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var notActivatedImage = new Image
            {
                Source = "ext-act",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0)
            };

            var notActivatedButton = new Button
            {
                Text = "Enable App Extension",
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

            notActivatedStackLayout.SetBinding<AppExtensionPageModel>(IsVisibleProperty, m => m.StartedAndNotActivated);

            // Activated 

            var activatedLabel = new Label
            {
                Text = "You're ready to log in!",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var activatedSublabel = new Label
            {
                Text = "In Safari, find bitwarden using the share icon (hint: scroll to the right on the bottom row of the menu).",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                Margin = new Thickness(0, 10, 0, 0)
            };

            var activatedImage = new Image
            {
                Source = "ext-use",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0)
            };

            var activatedButton = new Button
            {
                Text = "See Supported Apps",
                Command = new Command(() =>
                {
                    _googleAnalyticsService.TrackAppEvent("SeeSupportedApps");
                    Device.OpenUri(new Uri("https://bitwarden.com"));
                }),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var activatedButtonReenable = new Button
            {
                Text = "Re-enable App Extension",
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

            activatedStackLayout.SetBinding<AppExtensionPageModel>(IsVisibleProperty, m => m.StartedAndActivated);

            var stackLayout = new StackLayout
            {
                Children = { notStartedStackLayout, notActivatedStackLayout, activatedStackLayout },
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Close"));
            }

            Title = "App Extension";
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

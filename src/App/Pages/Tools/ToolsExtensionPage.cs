using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class ToolsExtensionPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;

        public ToolsExtensionPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
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
                Text = "To turn on bitwarden in Safari and other apps, tap \"more\" on the second row of the menu.",
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var notStartedImage = new Image
            {
                Source = "",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            var notStartedButton = new Button
            {
                Text = "Enable App Extension",
                Command = new Command(() => ShowExtension()),
                VerticalOptions = LayoutOptions.EndAndExpand,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var notStartedStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(30, 40),
                Children = { notStartedLabel, notStartedSublabel, notStartedImage, notStartedButton }
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
                Source = "",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            var notActivatedButton = new Button
            {
                Text = "Enable App Extension",
                Command = new Command(() => ShowExtension()),
                VerticalOptions = LayoutOptions.EndAndExpand,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var notActivatedStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(30, 40),
                Children = { notActivatedLabel, notActivatedSublabel, notActivatedImage, notActivatedButton }
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
                Source = "",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            var activatedButton = new Button
            {
                Text = "See Supported Apps",
                Command = new Command(() => Device.OpenUri(new Uri("https://bitwarden.com"))),
                VerticalOptions = LayoutOptions.EndAndExpand,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            var activatedButtonReenable = new Button
            {
                Text = "Re-enable App Extension",
                Command = new Command(() => ShowExtension()),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            var activatedStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 10,
                Padding = new Thickness(30, 40),
                Children = { activatedLabel, activatedSublabel, activatedImage, activatedButton, activatedButtonReenable }
            };

            activatedStackLayout.SetBinding<AppExtensionPageModel>(IsVisibleProperty, m => m.StartedAndActivated);

            var stackLayout = new StackLayout
            {
                Children = { notStartedStackLayout, notActivatedStackLayout, activatedStackLayout }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Close"));
            }

            Title = "App Extension";
            Content = stackLayout;
            BindingContext = Model;
        }

        private void ShowExtension()
        {
            MessagingCenter.Send(Application.Current, "ShowAppExtension", this);
        }

        public void EnabledExtension(bool enabled)
        {
            Model.Started = true;
            if(!Model.Activated && enabled)
            {
                Model.Activated = enabled;
            }
        }
    }
}

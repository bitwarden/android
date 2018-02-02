using System;
using Bit.App.Controls;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;
using Bit.App.Resources;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class ToolsAccessibilityServicePage : ExtendedContentPage
    {
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly IAppInfoService _appInfoService;
        private readonly IDeviceActionService _deviceActionService;
        private DateTime? _timerStarted = null;
        private TimeSpan _timerMaxLength = TimeSpan.FromMinutes(5);

        public ToolsAccessibilityServicePage()
        {
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _appInfoService = Resolver.Resolve<IAppInfoService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();

            Init();
        }

        public StackLayout EnabledStackLayout { get; set; }
        public StackLayout DisabledStackLayout { get; set; }
        public ScrollView ScrollView { get; set; }

        public void Init()
        {
            var enabledFs = new FormattedString();
            var statusSpan = new Span { Text = string.Concat(AppResources.Status, " ") };
            enabledFs.Spans.Add(statusSpan);
            enabledFs.Spans.Add(new Span
            {
                Text = AppResources.Enabled,
                ForegroundColor = Color.Green,
                FontAttributes = FontAttributes.Bold,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            });

            var statusEnabledLabel = new Label
            {
                FormattedText = enabledFs,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                TextColor = Color.Black
            };

            var disabledFs = new FormattedString();
            disabledFs.Spans.Add(statusSpan);
            disabledFs.Spans.Add(new Span
            {
                Text = AppResources.Disabled,
                ForegroundColor = Color.FromHex("c62929"),
                FontAttributes = FontAttributes.Bold,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            });

            var statusDisabledLabel = new Label
            {
                FormattedText = disabledFs,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                TextColor = Color.Black
            };

            var step1Label = new Label
            {
                Text = AppResources.BitwardenAutofillServiceStep1,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                TextColor = Color.Black
            };

            var step1Image = new CachedImage
            {
                Source = "accessibility_step1",
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, 20, 0, 0),
                WidthRequest = 300,
                HeightRequest = 98
            };

            var step2Label = new Label
            {
                Text = AppResources.BitwardenAutofillServiceStep2,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                TextColor = Color.Black
            };

            var step2Image = new CachedImage
            {
                Source = "accessibility_step2",
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, 20, 0, 0),
                WidthRequest = 300,
                HeightRequest = 67
            };

            var stepsStackLayout = new StackLayout
            {
                Children = { statusDisabledLabel, step1Image, step1Label, step2Image, step2Label },
                Orientation = StackOrientation.Vertical,
                Spacing = 10,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            var notificationsLabel = new Label
            {
                Text = AppResources.BitwardenAutofillServiceNotification,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                TextColor = Color.Black
            };

            var tapNotificationImage = new CachedImage
            {
                Source = "accessibility_notification.png",
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, 20, 0, 0),
                WidthRequest = 300,
                HeightRequest = 74
            };

            var tapNotificationIcon = new CachedImage
            {
                Source = "accessibility_notification_icon.png",
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, 20, 0, 0),
                WidthRequest = 300,
                HeightRequest = 54
            };

            var notificationsStackLayout = new StackLayout
            {
                Children = { statusEnabledLabel, tapNotificationIcon, tapNotificationImage, notificationsLabel },
                Orientation = StackOrientation.Vertical,
                Spacing = 10,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center
            };

            DisabledStackLayout = new StackLayout
            {
                Children = { BuildServiceLabel(), stepsStackLayout, BuildGoButton() },
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            EnabledStackLayout = new StackLayout
            {
                Children = { BuildServiceLabel(), notificationsStackLayout, BuildGoButton() },
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            ScrollView = new ScrollView { Content = DisabledStackLayout };
            Title = AppResources.AutofillAccessibilityService;
            Content = ScrollView;
        }

        protected override void OnAppearing()
        {
            UpdateEnabled();
            _timerStarted = DateTime.Now;
            Device.StartTimer(new TimeSpan(0, 0, 3), () =>
            {
                System.Diagnostics.Debug.WriteLine("Check timer on accessibility");
                if(_timerStarted == null || (DateTime.Now - _timerStarted) > _timerMaxLength)
                {
                    return false;
                }

                UpdateEnabled();
                return true;
            });

            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            _timerStarted = null;
            base.OnDisappearing();
        }

        private void UpdateEnabled()
        {
            ScrollView.Content = _appInfoService.AutofillAccessibilityServiceEnabled ?
                EnabledStackLayout : DisabledStackLayout;
        }

        private Label BuildServiceLabel()
        {
            return new Label
            {
                Text = AppResources.AutofillAccessibilityDescription,
                VerticalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label))
            };
        }

        private ExtendedButton BuildGoButton()
        {
            return new ExtendedButton
            {
                Text = AppResources.BitwardenAutofillServiceOpenAccessibilitySettings,
                Command = new Command(() =>
                {
                    _googleAnalyticsService.TrackAppEvent("OpenAccessibilitySettings");
                    _deviceActionService.OpenAccessibilitySettings();
                }),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"]
            };
        }
    }
}

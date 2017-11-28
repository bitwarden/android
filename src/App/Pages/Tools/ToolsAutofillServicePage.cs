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
        private readonly IAppInfoService _appInfoService;
        private readonly IDeviceActionService _deviceActionService;
        private bool _pageDisappeared = false;

        public ToolsAutofillServicePage()
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
                TextColor = Color.Black,
                VerticalOptions = LayoutOptions.CenterAndExpand
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
                TextColor = Color.Black,
                VerticalOptions = LayoutOptions.CenterAndExpand
            };

            var goButton = new ExtendedButton
            {
                Text = AppResources.BitwardenAutofillServiceOpenAutofillSettings,
                Command = new Command(() =>
                {
                    _googleAnalyticsService.TrackAppEvent("OpenAutofillSettings");
                    _deviceActionService.OpenAutofillSettings();
                }),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primary"],
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Button))
            };

            DisabledStackLayout = new StackLayout
            {
                Children = { BuildServiceLabel(), statusDisabledLabel, goButton, BuildAccessibilityButton() },
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            EnabledStackLayout = new StackLayout
            {
                Children = { BuildServiceLabel(), statusEnabledLabel, BuildAccessibilityButton() },
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            ScrollView = new ScrollView { Content = DisabledStackLayout };

            UpdateEnabled();
            Device.StartTimer(new TimeSpan(0, 0, 3), () =>
            {
                if(_pageDisappeared)
                {
                    return false;
                }

                UpdateEnabled();
                return true;
            });

            Title = AppResources.AutofillService;
            Content = ScrollView;
        }

        protected override void OnAppearing()
        {
            _pageDisappeared = false;
            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            _pageDisappeared = true;
            base.OnDisappearing();
        }

        private void UpdateEnabled()
        {
            ScrollView.Content = _appInfoService.AutofillServiceEnabled ? EnabledStackLayout : DisabledStackLayout;
        }

        private Label BuildServiceLabel()
        {
            return new Label
            {
                Text = AppResources.AutofillServiceDescription,
                VerticalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label))
            };
        }

        private ExtendedButton BuildAccessibilityButton()
        {
            return new ExtendedButton
            {
                Text = AppResources.AutofillAccessibilityService,
                Command = new Command(async () =>
                {
                    await Navigation.PushAsync(new ToolsAccessibilityServicePage());
                }),
                VerticalOptions = LayoutOptions.End,
                HorizontalOptions = LayoutOptions.Fill,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                Uppercase = false,
                BackgroundColor = Color.Transparent
            };
        }
    }
}

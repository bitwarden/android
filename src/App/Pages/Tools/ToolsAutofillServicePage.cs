using System;
using Bit.App.Controls;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;
using Bit.App.Resources;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class ToolsAutofillServicePage : ExtendedContentPage
    {
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly IAppInfoService _appInfoService;
        private readonly IDeviceActionService _deviceActionService;
        private DateTime? _timerStarted = null;
        private TimeSpan _timerMaxLength = TimeSpan.FromMinutes(5);

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
                TextColor = Color.Black
            };

            var enableImage = new CachedImage
            {
                Source = "autofill_enable.png",
                HorizontalOptions = LayoutOptions.Center,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                WidthRequest = 300,
                HeightRequest = 118
            };

            var useImage = new CachedImage
            {
                Source = "autofill_use.png",
                HorizontalOptions = LayoutOptions.Center,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                WidthRequest = 300,
                HeightRequest = 128
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
                Style = (Style)Application.Current.Resources["btn-primary"]
            };

            DisabledStackLayout = new StackLayout
            {
                Children = { BuildServiceLabel(), statusDisabledLabel, enableImage, goButton },
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            EnabledStackLayout = new StackLayout
            {
                Children = { BuildServiceLabel(), statusEnabledLabel, useImage },
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 30),
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            ScrollView = new ScrollView { Content = DisabledStackLayout };
            Title = AppResources.AutofillService;
            Content = ScrollView;
        }

        protected override void OnAppearing()
        {
            UpdateEnabled();
            _timerStarted = DateTime.Now;
            Device.StartTimer(new TimeSpan(0, 0, 2), () =>
            {
                System.Diagnostics.Debug.WriteLine("Check timer on autofill");
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
    }
}

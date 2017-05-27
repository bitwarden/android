using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using Acr.UserDialogs;
using Plugin.Settings.Abstractions;
using Plugin.Fingerprint.Abstractions;
using PushNotification.Plugin.Abstractions;

namespace Bit.App.Pages
{
    public class SettingsFeaturesPage : ExtendedContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly IFingerprint _fingerprint;
        private readonly IPushNotification _pushNotification;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public SettingsFeaturesPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _pushNotification = Resolver.Resolve<IPushNotification>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        private StackLayout StackLayout { get; set; }
        private ExtendedSwitchCell AnalyticsCell { get; set; }
        private Label AnalyticsLabel { get; set; }

        private void Init()
        {
            AnalyticsCell = new ExtendedSwitchCell
            {
                Text = AppResources.DisableGA,
                On = _settings.GetValueOrDefault(Constants.SettingGaOptOut, false)
            };

            var analyticsTable = new FormTableView
            {
                Root = new TableRoot
                {
                    new TableSection(" ")
                    {
                        AnalyticsCell
                    }
                }
            };

            AnalyticsLabel = new Label
            {
                Text = AppResources.DisbaleGADescription,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                Margin = new Thickness(15, (this.IsLandscape() ? 5 : 0), 15, 25)
            };

            StackLayout = new StackLayout
            {
                Children = { analyticsTable, AnalyticsLabel },
                Spacing = 0
            };

            var scrollView = new ScrollView
            {
                Content = StackLayout
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                analyticsTable.RowHeight = -1;
                analyticsTable.EstimatedRowHeight = 70;
            }

            Title = AppResources.Features;
            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            
            AnalyticsCell.OnChanged += AnalyticsCell_Changed;
            StackLayout.LayoutChanged += Layout_LayoutChanged;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            
            AnalyticsCell.OnChanged -= AnalyticsCell_Changed;
            StackLayout.LayoutChanged -= Layout_LayoutChanged;
        }

        private void Layout_LayoutChanged(object sender, EventArgs e)
        {
            AnalyticsLabel.WidthRequest = StackLayout.Bounds.Width - AnalyticsLabel.Bounds.Left * 2;
        }

        private void AnalyticsCell_Changed(object sender, ToggledEventArgs e)
        {
            var cell = sender as ExtendedSwitchCell;
            if(cell == null)
            {
                return;
            }

            _settings.AddOrUpdateValue(Constants.SettingGaOptOut, cell.On);
            _googleAnalyticsService.SetAppOptOut(cell.On);
        }

        private class FormTableView : ExtendedTableView
        {
            public FormTableView()
            {
                Intent = TableIntent.Settings;
                EnableScrolling = false;
                HasUnevenRows = true;
                EnableSelection = true;
                VerticalOptions = LayoutOptions.Start;
                NoFooter = true;
            }
        }
    }
}

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
        private readonly IAppSettingsService _appSettings;
        private readonly IFingerprint _fingerprint;
        private readonly IPushNotification _pushNotification;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public SettingsFeaturesPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
            _appSettings = Resolver.Resolve<IAppSettingsService>();
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _pushNotification = Resolver.Resolve<IPushNotification>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        private StackLayout StackLayout { get; set; }
        private ExtendedSwitchCell AnalyticsCell { get; set; }
        private Label AnalyticsLabel { get; set; }
        private ExtendedSwitchCell AutofillPersistNotificationCell { get; set; }
        private Label AutofillPersistNotificationLabel { get; set; }
        private ExtendedSwitchCell AutofillPasswordFieldCell { get; set; }
        private Label AutofillPasswordFieldLabel { get; set; }
        private ExtendedSwitchCell AutofillAlwaysCell { get; set; }
        private Label AutofillAlwaysLabel { get; set; }

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

            if(Device.OS == TargetPlatform.Android)
            {
                AutofillAlwaysCell = new ExtendedSwitchCell
                {
                    Text = AppResources.AutofillAlways,
                    On = !_appSettings.AutofillPersistNotification && !_appSettings.AutofillPasswordField
                };

                var autofillAlwaysTable = new FormTableView
                {
                    Root = new TableRoot
                    {
                        new TableSection(AppResources.AutofillService)
                        {
                            AutofillAlwaysCell
                        }
                    }
                };

                AutofillAlwaysLabel = new FormTableLabel(this)
                {
                    Text = AppResources.AutofillAlwaysDescription
                };

                AutofillPersistNotificationCell = new ExtendedSwitchCell
                {
                    Text = AppResources.AutofillPersistNotification,
                    On = _appSettings.AutofillPersistNotification
                };

                var autofillPersistNotificationTable = new FormTableView
                {
                    NoHeader = true,
                    Root = new TableRoot
                    {
                        new TableSection(" ")
                        {
                            AutofillPersistNotificationCell
                        }
                    }
                };

                AutofillPersistNotificationLabel = new FormTableLabel(this)
                {
                    Text = AppResources.AutofillPersistNotificationDescription
                };

                AutofillPasswordFieldCell = new ExtendedSwitchCell
                {
                    Text = AppResources.AutofillPasswordField,
                    On = _appSettings.AutofillPasswordField
                };

                var autofillPasswordFieldTable = new FormTableView
                {
                    NoHeader = true,
                    Root = new TableRoot
                    {
                        new TableSection(" ")
                        {
                            AutofillPasswordFieldCell
                        }
                    }
                };

                AutofillPasswordFieldLabel = new FormTableLabel(this)
                {
                    Text = AppResources.AutofillPasswordFieldDescription
                };

                StackLayout.Children.Add(autofillAlwaysTable);
                StackLayout.Children.Add(AutofillAlwaysLabel);
                StackLayout.Children.Add(autofillPasswordFieldTable);
                StackLayout.Children.Add(AutofillPasswordFieldLabel);
                StackLayout.Children.Add(autofillPersistNotificationTable);
                StackLayout.Children.Add(AutofillPersistNotificationLabel);
            }

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

            if(Device.RuntimePlatform == Device.Android)
            {
                AutofillAlwaysCell.OnChanged += AutofillAlwaysCell_OnChanged;
                AutofillPasswordFieldCell.OnChanged += AutofillPasswordFieldCell_OnChanged;
                AutofillPersistNotificationCell.OnChanged += AutofillPersistNotificationCell_OnChanged;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            AnalyticsCell.OnChanged -= AnalyticsCell_Changed;
            StackLayout.LayoutChanged -= Layout_LayoutChanged;

            if(Device.RuntimePlatform == Device.Android)
            {
                AutofillAlwaysCell.OnChanged -= AutofillAlwaysCell_OnChanged;
                AutofillPasswordFieldCell.OnChanged -= AutofillPasswordFieldCell_OnChanged;
                AutofillPersistNotificationCell.OnChanged -= AutofillPersistNotificationCell_OnChanged;
            }
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

        private void AutofillAlwaysCell_OnChanged(object sender, ToggledEventArgs e)
        {
            var cell = sender as ExtendedSwitchCell;
            if(cell == null)
            {
                return;
            }

            if(cell.On)
            {
                AutofillPasswordFieldCell.On = false;
                AutofillPersistNotificationCell.On = false;
                _appSettings.AutofillPersistNotification = false;
                _appSettings.AutofillPasswordField = false;
            }
        }

        private void AutofillPersistNotificationCell_OnChanged(object sender, ToggledEventArgs e)
        {
            var cell = sender as ExtendedSwitchCell;
            if(cell == null)
            {
                return;
            }

            _appSettings.AutofillPersistNotification = cell.On;
            if(cell.On)
            {
                AutofillPasswordFieldCell.On = false;
                AutofillAlwaysCell.On = false;
            }
        }

        private void AutofillPasswordFieldCell_OnChanged(object sender, ToggledEventArgs e)
        {
            var cell = sender as ExtendedSwitchCell;
            if(cell == null)
            {
                return;
            }

            _appSettings.AutofillPasswordField = cell.On;
            if(cell.On)
            {
                AutofillPersistNotificationCell.On = false;
                AutofillAlwaysCell.On = false;
            }
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

        private class FormTableLabel : Label
        {
            public FormTableLabel(Page page)
            {
                LineBreakMode = LineBreakMode.WordWrap;
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label));
                Style = (Style)Application.Current.Resources["text-muted"];
                Margin = new Thickness(15, (page.IsLandscape() ? 5 : 0), 15, 25);
            }
        }
    }
}

using System;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Core.Renderers;
using Bit.iOS.Core.Utilities;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(TabbedPage), typeof(CustomTabbedRenderer))]

namespace Bit.iOS.Core.Renderers
{
    public class CustomTabbedRenderer : TabbedRenderer
    {
        private IBroadcasterService _broadcasterService;
        private UITabBarItem _previousSelectedItem;

        public CustomTabbedRenderer()
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _broadcasterService.Subscribe(nameof(CustomTabbedRenderer), (message) =>
            {
                if (message.Command is ThemeManager.UPDATED_THEME_MESSAGE_KEY)
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        iOSCoreHelpers.AppearanceAdjustments();
                        UpdateTabBarAppearance();
                    });
                }
            });
        }

        protected override void OnElementChanged(VisualElementChangedEventArgs e)
        {
            base.OnElementChanged(e);
            TabBar.Translucent = false;
            TabBar.Opaque = true;
            UpdateTabBarAppearance();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);

            if (SelectedIndex < TabBar.Items.Length)
            {
                _previousSelectedItem = TabBar.Items[SelectedIndex];
            }
        }

        public override void ItemSelected(UITabBar tabbar, UITabBarItem item)
        {
            if (_previousSelectedItem == item && Element is TabsPage tabsPage)
            {
                tabsPage.OnPageReselected();
            }
            _previousSelectedItem = item;
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _broadcasterService.Unsubscribe(nameof(CustomTabbedRenderer));
            }
            base.Dispose(disposing);
        }

        private void UpdateTabBarAppearance()
        {
            // https://developer.apple.com/forums/thread/682420
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            if (deviceActionService.SystemMajorVersion() >= 15)
            {
                var appearance = new UITabBarAppearance();
                appearance.ConfigureWithOpaqueBackground();
                appearance.BackgroundColor = ThemeHelpers.TabBarBackgroundColor;
                appearance.StackedLayoutAppearance.Normal.IconColor = ThemeHelpers.TabBarItemColor;
                appearance.StackedLayoutAppearance.Normal.TitleTextAttributes =
                    new UIStringAttributes { ForegroundColor = ThemeHelpers.TabBarItemColor };
                TabBar.StandardAppearance = appearance;
                TabBar.ScrollEdgeAppearance = TabBar.StandardAppearance;
            }
        }
    }
}

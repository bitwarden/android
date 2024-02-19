using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using Microsoft.Maui.Controls.Handlers.Compatibility;
using Microsoft.Maui.Controls.Platform;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public partial class CustomTabbedHandler : TabbedRenderer
    {
        private IBroadcasterService _broadcasterService;
        private UITabBarItem? _previousSelectedItem;

        public CustomTabbedHandler()
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _broadcasterService.Subscribe(nameof(CustomTabbedHandler), (message) =>
            {
                if (message.Command is ThemeManager.UPDATED_THEME_MESSAGE_KEY)
                {
                    MainThread.BeginInvokeOnMainThread(() =>
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

            if(TabBar?.Items != null)
            {
                if (SelectedIndex < TabBar.Items.Length)
                {
                    _previousSelectedItem = TabBar.Items[SelectedIndex];
                }
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
                _broadcasterService.Unsubscribe(nameof(CustomTabbedHandler));
            }
            base.Dispose(disposing);
        }

        private void UpdateTabBarAppearance()
        {
            // https://developer.apple.com/forums/thread/682420
            if (UIDevice.CurrentDevice.CheckSystemVersion(15,0))
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

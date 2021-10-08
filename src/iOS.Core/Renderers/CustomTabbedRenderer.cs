using Bit.App.Abstractions;
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

        public CustomTabbedRenderer()
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _broadcasterService.Subscribe(nameof(CustomTabbedRenderer), async (message) =>
            {
                if (message.Command == "updatedTheme")
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

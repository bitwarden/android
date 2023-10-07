#if IOS || MACCATALYST
using PlatformView = WebKit.WKWebView;
#elif ANDROID
using PlatformView = Android.Webkit.WebView;
#elif (NETSTANDARD || !PLATFORM) || (NET6_0_OR_GREATER && !IOS && !ANDROID)
using PlatformView = System.Object;
#endif

using Bit.App.Controls;
using Microsoft.Maui.Handlers;

namespace Bit.App.Handlers
{
    public partial class HybridWebViewHandler
    {
        public static PropertyMapper<HybridWebView, HybridWebViewHandler> PropertyMapper = new PropertyMapper<HybridWebView, HybridWebViewHandler>(ViewHandler.ViewMapper)
        {
            [nameof(HybridWebView.Uri)] = MapUri
        };

        public HybridWebViewHandler() : base(PropertyMapper)
        {
        }
    }
}

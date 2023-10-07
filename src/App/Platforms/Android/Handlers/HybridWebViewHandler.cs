using Bit.App.Controls;
using Java.Interop;
using JetBrains.Annotations;
using Microsoft.Maui.Handlers;
using AWebkit = Android.Webkit;

namespace Bit.App.Handlers
{
    public partial class HybridWebViewHandler : ViewHandler<HybridWebView, AWebkit.WebView>
    {
        private const string JSFunction = "function invokeCSharpAction(data){jsBridge.invokeAction(data);}";

        public HybridWebViewHandler([NotNull] IPropertyMapper mapper, CommandMapper commandMapper = null) : base(mapper, commandMapper)
        {
        }

        protected override AWebkit.WebView CreatePlatformView()
        {
            var context = MauiContext?.Context ?? throw new InvalidOperationException($"Context cannot be null here");
            var webView = new AWebkit.WebView(context);
            webView.Settings.JavaScriptEnabled = true;
            webView.SetWebViewClient(new JSWebViewClient(string.Format("javascript: {0}", JSFunction)));
            return webView;
        }

        public static void MapUri(HybridWebViewHandler handler, HybridWebView view)
        {
            if (view != null && view.Uri != null)
            {
                handler?.PlatformView?.LoadUrl(view.Uri);
            }
        }

        protected override void ConnectHandler(AWebkit.WebView platformView)
        {
            platformView?.AddJavascriptInterface(new JSBridge(this), "jsBridge");
            platformView?.LoadUrl(VirtualView?.Uri);

            base.ConnectHandler(platformView);
        }

        //Currently the Disconnect Handler needs to be manually called from the App: https://github.com/dotnet/maui/issues/3604
        protected override void DisconnectHandler(AWebkit.WebView platformView)
        {
            platformView?.RemoveJavascriptInterface("jsBridge");
            platformView?.Dispose();
            VirtualView?.Cleanup();

            base.DisconnectHandler(platformView);
        }

        internal void InvokeActionOnVirtual(string data)
        {
            VirtualView?.InvokeAction(data);
        }
    }

    public class JSBridge : Java.Lang.Object
    {
        private readonly WeakReference<HybridWebViewHandler> _hybridWebViewRenderer;

        public JSBridge(HybridWebViewHandler hybridRenderer)
        {
            _hybridWebViewRenderer = new WeakReference<HybridWebViewHandler>(hybridRenderer);
        }

        [AWebkit.JavascriptInterface]
        [Export("invokeAction")]
        public void InvokeAction(string data)
        {
            if (_hybridWebViewRenderer != null &&_hybridWebViewRenderer.TryGetTarget(out HybridWebViewHandler hybridRenderer))
            {
                hybridRenderer?.InvokeActionOnVirtual(data);
            }
        }
    }

    public class JSWebViewClient : AWebkit.WebViewClient
    {
        private readonly string _javascript;

        public JSWebViewClient(string javascript)
        {
            _javascript = javascript;
        }

        public override void OnPageFinished(AWebkit.WebView view, string url)
        {
            base.OnPageFinished(view, url);
            if (view != null)
            {
                view.EvaluateJavascript(_javascript, null);
            }
        }
    }
}

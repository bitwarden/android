using System.ComponentModel;
using Android.Content;
using Android.Webkit;
using Bit.App.Controls;
using Java.Interop;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;
using AWebkit = Android.Webkit;

namespace Bit.App.Droid.Renderers
{
    public class HybridWebViewRenderer : ViewRenderer<HybridWebView, AWebkit.WebView>
    {
        private const string JSFunction = "function invokeCSharpAction(data){jsBridge.invokeAction(data);}";

        private readonly Context _context;

        public HybridWebViewRenderer(Context context)
            : base(context)
        {
            _context = context;
        }

        protected override void OnElementChanged(ElementChangedEventArgs<HybridWebView> e)
        {
            base.OnElementChanged(e);

            if (Control == null)
            {
                var webView = new AWebkit.WebView(_context);
                webView.Settings.JavaScriptEnabled = true;
                webView.SetWebViewClient(new JSWebViewClient(string.Format("javascript: {0}", JSFunction)));
                SetNativeControl(webView);
            }
            if (e.OldElement != null)
            {
                Control.RemoveJavascriptInterface("jsBridge");
                var hybridWebView = e.OldElement as HybridWebView;
                hybridWebView.Cleanup();
            }
            if (e.NewElement != null)
            {
                Control.AddJavascriptInterface(new JSBridge(this), "jsBridge");
                Control.LoadUrl(Element.Uri);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if (e.PropertyName == HybridWebView.UriProperty.PropertyName)
            {
                Control.LoadUrl(Element.Uri);
            }
        }

        public class JSBridge : Java.Lang.Object
        {
            private readonly WeakReference<HybridWebViewRenderer> _hybridWebViewRenderer;

            public JSBridge(HybridWebViewRenderer hybridRenderer)
            {
                _hybridWebViewRenderer = new WeakReference<HybridWebViewRenderer>(hybridRenderer);
            }

            [JavascriptInterface]
            [Export("invokeAction")]
            public void InvokeAction(string data)
            {
                if (_hybridWebViewRenderer != null &&
                    _hybridWebViewRenderer.TryGetTarget(out HybridWebViewRenderer hybridRenderer))
                {
                    hybridRenderer.Element.InvokeAction(data);
                }
            }
        }

        public class JSWebViewClient : WebViewClient
        {
            private readonly string _javascript;

            public JSWebViewClient(string javascript)
            {
                _javascript = javascript;

            }

            public override void OnPageFinished(AWebkit.WebView view, string url)
            {
                base.OnPageFinished(view, url);
                view.EvaluateJavascript(_javascript, null);
            }
        }
    }
}

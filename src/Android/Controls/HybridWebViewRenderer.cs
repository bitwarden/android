using System;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Android.Webkit;
using AWebkit = Android.Webkit;
using Java.Interop;

[assembly: ExportRenderer(typeof(HybridWebView), typeof(HybridWebViewRenderer))]
namespace Bit.Android.Controls
{
    public class HybridWebViewRenderer : ViewRenderer<HybridWebView, AWebkit.WebView>
    {
        private const string JSFunction = "function invokeCSharpAction(data){jsBridge.invokeAction(data);}";

        protected override void OnElementChanged(ElementChangedEventArgs<HybridWebView> e)
        {
            base.OnElementChanged(e);

            if(Control == null)
            {
                var webView = new AWebkit.WebView(Forms.Context);
                webView.Settings.JavaScriptEnabled = true;
                SetNativeControl(webView);
            }

            if(e.OldElement != null)
            {
                Control.RemoveJavascriptInterface("jsBridge");
                var hybridWebView = e.OldElement as HybridWebView;
                hybridWebView.Cleanup();
            }

            if(e.NewElement != null)
            {
                Control.AddJavascriptInterface(new JSBridge(this), "jsBridge");
                Control.LoadUrl(Element.Uri);
                InjectJS(JSFunction);
            }
        }

        private void InjectJS(string script)
        {
            if(Control != null)
            {
                Control.LoadUrl(string.Format("javascript: {0}", script));
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
                HybridWebViewRenderer hybridRenderer;
                if(_hybridWebViewRenderer != null && _hybridWebViewRenderer.TryGetTarget(out hybridRenderer))
                {
                    hybridRenderer.Element.InvokeAction(data);
                }
            }
        }
    }
}

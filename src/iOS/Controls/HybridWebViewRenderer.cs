using System.IO;
using Foundation;
using WebKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;
using Bit.App.Controls;
using Bit.iOS.Controls;

[assembly: ExportRenderer(typeof(HybridWebView), typeof(HybridWebViewRenderer))]
namespace Bit.iOS.Controls
{
    public class HybridWebViewRenderer : ViewRenderer<HybridWebView, WKWebView>, IWKScriptMessageHandler
    {
        private const string JSFunction =
            "function invokeCSharpAction(data){window.webkit.messageHandlers.invokeAction.postMessage(data);}";

        private WKUserContentController _userController;

        protected override void OnElementChanged(ElementChangedEventArgs<HybridWebView> e)
        {
            base.OnElementChanged(e);

            if(Control == null)
            {
                _userController = new WKUserContentController();
                var script = new WKUserScript(new NSString(JSFunction), WKUserScriptInjectionTime.AtDocumentEnd, false);
                _userController.AddUserScript(script);
                _userController.AddScriptMessageHandler(this, "invokeAction");

                var config = new WKWebViewConfiguration { UserContentController = _userController };
                var webView = new WKWebView(Frame, config);
                SetNativeControl(webView);
            }

            if(e.OldElement != null)
            {
                _userController.RemoveAllUserScripts();
                _userController.RemoveScriptMessageHandler("invokeAction");
                var hybridWebView = e.OldElement as HybridWebView;
                hybridWebView.Cleanup();
            }

            if(e.NewElement != null)
            {
                Control.LoadRequest(new NSUrlRequest(new NSUrl(Element.Uri)));
            }
        }

        public void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage message)
        {
            Element.InvokeAction(message.Body.ToString());
        }
    }
}

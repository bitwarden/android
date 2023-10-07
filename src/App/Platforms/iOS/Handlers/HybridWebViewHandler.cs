using System.Diagnostics.CodeAnalysis;
using Bit.App.Controls;
using CoreGraphics;
using Foundation;
using Microsoft.Maui.Handlers;
using WebKit;

namespace Bit.App.Handlers
{
    public partial class HybridWebViewHandler : ViewHandler<HybridWebView, WebKit.WKWebView>
    {
        private const string JSFunction =
            "function invokeCSharpAction(data){window.webkit.messageHandlers.invokeAction.postMessage(data);}";

        private WKUserContentController _userController;

        public HybridWebViewHandler([NotNull] IPropertyMapper mapper, CommandMapper commandMapper = null) : base(mapper, commandMapper)
        {
        }

        protected override WKWebView CreatePlatformView()
        {
            _userController = new WKUserContentController();
            var script = new WKUserScript(new NSString(JSFunction), WKUserScriptInjectionTime.AtDocumentEnd, false);
            _userController.AddUserScript(script);
            _userController.AddScriptMessageHandler(new WebViewScriptMessageHandler(InvokeAction), "invokeAction");

            var config = new WKWebViewConfiguration { UserContentController = _userController };
            var webView = new WKWebView(CGRect.Empty, config);
            return webView;
        }

        public static void MapUri(HybridWebViewHandler handler, HybridWebView view)
        {
            if (handler != null && view?.Uri != null)
            {
                handler?.PlatformView?.LoadRequest(new NSUrlRequest(new NSUrl(view.Uri)));
            }
        }

        protected override void ConnectHandler(WKWebView platformView)
        {
            if (VirtualView?.Uri != null)
            {
                platformView?.LoadRequest(new NSUrlRequest(new NSUrl(VirtualView?.Uri)));
            }
            base.ConnectHandler(platformView);
        }

        protected override void DisconnectHandler(WKWebView platformView)
        {
            _userController.RemoveAllUserScripts();
            _userController.RemoveScriptMessageHandler("invokeAction");
            platformView?.Dispose();
            VirtualView?.Cleanup();
            _userController = null;

            base.DisconnectHandler(platformView);
        }

        private void InvokeAction(WKScriptMessage message)
        {
            if(message?.Body != null)
            {
                VirtualView?.InvokeAction(message.Body.ToString());
            }
        }
    }

    public class WebViewScriptMessageHandler : NSObject, IWKScriptMessageHandler
    {
        private Action<WKScriptMessage> _messageReceivedAction;

        public WebViewScriptMessageHandler(Action<WKScriptMessage> messageReceivedAction)
        {
            _messageReceivedAction = messageReceivedAction ?? throw new ArgumentNullException(nameof(messageReceivedAction));
        }

        public void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage message)
        {
            _messageReceivedAction(message);
        }
    }
}

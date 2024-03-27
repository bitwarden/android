// This is a copy from MAUI Essentials WebAuthenticator with a fix for getting UIWindow without Scenes.

#if IOS
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using AuthenticationServices;
using Foundation;
using SafariServices;
using ObjCRuntime;
using UIKit;
using WebKit;
using Microsoft.Maui.Authentication;
using Microsoft.Maui.ApplicationModel;
using Bit.Core.Services;

namespace Bit.Core.Utilities.MAUI
{
    partial class WebAuthenticatorImplementation : IWebAuthenticator, IPlatformWebAuthenticatorCallback
    {
#if IOS
		const int asWebAuthenticationSessionErrorCodeCanceledLogin = 1;
		const string asWebAuthenticationSessionErrorDomain = "com.apple.AuthenticationServices.WebAuthenticationSession";

		const int sfAuthenticationErrorCanceledLogin = 1;
		const string sfAuthenticationErrorDomain = "com.apple.SafariServices.Authentication";
#endif

        TaskCompletionSource<WebAuthenticatorResult> tcsResponse;
        UIViewController currentViewController;
        Uri redirectUri;
        WebAuthenticatorOptions currentOptions;

#if IOS
		ASWebAuthenticationSession was;
		SFAuthenticationSession sf;
#endif

		public async Task<WebAuthenticatorResult> AuthenticateAsync(WebAuthenticatorOptions webAuthenticatorOptions)
        {
            currentOptions = webAuthenticatorOptions;
            var url = webAuthenticatorOptions?.Url;
            var callbackUrl = webAuthenticatorOptions?.CallbackUrl;
            var prefersEphemeralWebBrowserSession = webAuthenticatorOptions?.PrefersEphemeralWebBrowserSession ?? false;

            if (!VerifyHasUrlSchemeOrDoesntRequire(callbackUrl.Scheme))
                throw new InvalidOperationException("You must register your URL Scheme handler in your app's Info.plist.");

            // Cancel any previous task that's still pending
            if (tcsResponse?.Task != null && !tcsResponse.Task.IsCompleted)
                tcsResponse.TrySetCanceled();

            tcsResponse = new TaskCompletionSource<WebAuthenticatorResult>();
            redirectUri = callbackUrl;
            var scheme = redirectUri.Scheme;

#if IOS
			void AuthSessionCallback(NSUrl cbUrl, NSError error)
			{
				if (error == null)
					OpenUrlCallback(cbUrl);
				else if (error.Domain == asWebAuthenticationSessionErrorDomain && error.Code == asWebAuthenticationSessionErrorCodeCanceledLogin)
					tcsResponse.TrySetCanceled();
				else if (error.Domain == sfAuthenticationErrorDomain && error.Code == sfAuthenticationErrorCanceledLogin)
					tcsResponse.TrySetCanceled();
				else
					tcsResponse.TrySetException(new NSErrorException(error));

				was = null;
				sf = null;
			}

			if (OperatingSystem.IsIOSVersionAtLeast(12))
			{
				was = new ASWebAuthenticationSession(MAUI.WebUtils.GetNativeUrl(url), scheme, AuthSessionCallback);

				if (OperatingSystem.IsIOSVersionAtLeast(13))
                {
                    var ctx = new ContextProvider(webAuthenticatorOptions.ShouldUseSharedApplicationKeyWindow
						? GetWorkaroundedUIWindow()
                        : WindowStateManager.Default.GetCurrentUIWindow());
					was.PresentationContextProvider = ctx;
					was.PrefersEphemeralWebBrowserSession = prefersEphemeralWebBrowserSession;
				}
				else if (prefersEphemeralWebBrowserSession)
				{
					ClearCookies();
				}

				using (was)
				{
#pragma warning disable CA1416 // Analyzer bug https://github.com/dotnet/roslyn-analyzers/issues/5938
					was.Start();
#pragma warning restore CA1416
					return await tcsResponse.Task;
				}
			}

			if (prefersEphemeralWebBrowserSession)
				ClearCookies();

#pragma warning disable CA1422 // 'SFAuthenticationSession' is obsoleted on: 'ios' 12.0 and later
			if (OperatingSystem.IsIOSVersionAtLeast(11))
			{
				sf = new SFAuthenticationSession(MAUI.WebUtils.GetNativeUrl(url), scheme, AuthSessionCallback);
				using (sf)
				{
					sf.Start();
					return await tcsResponse.Task;
				}
			}
#pragma warning restore CA1422

			// This is only on iOS9+ but we only support 10+ in Essentials anyway
			var controller = new SFSafariViewController(MAUI.WebUtils.GetNativeUrl(url), false)
			{
				Delegate = new NativeSFSafariViewControllerDelegate
				{
					DidFinishHandler = (svc) =>
					{
						// Cancel our task if it wasn't already marked as completed
						if (!(tcsResponse?.Task?.IsCompleted ?? true))
							tcsResponse.TrySetCanceled();
					}
				},
			};

			currentViewController = controller;
			await WindowStateManager.Default.GetCurrentUIViewController().PresentViewControllerAsync(controller, true);
#else
            var opened = UIApplication.SharedApplication.OpenUrl(url);
            if (!opened)
                tcsResponse.TrySetException(new Exception("Error opening Safari"));
#endif

            return await tcsResponse.Task;
        }

        private UIWindow GetWorkaroundedUIWindow(bool throwIfNull = false)
        {
            var window = UIApplication.SharedApplication.KeyWindow;

            if (window != null && window.WindowLevel == UIWindowLevel.Normal)
                return window;

            if (window == null)
            {
                window = UIApplication.SharedApplication
                    .Windows
                    .OrderByDescending(w => w.WindowLevel)
                    .FirstOrDefault(w => w.RootViewController != null && w.WindowLevel == UIWindowLevel.Normal);
            }

            if (throwIfNull && window == null)
                throw new InvalidOperationException("Could not find current window.");

            return window;

        }

        void ClearCookies()
        {
            NSUrlCache.SharedCache.RemoveAllCachedResponses();

#if IOS
			if (OperatingSystem.IsIOSVersionAtLeast(11))
			{
				WKWebsiteDataStore.DefaultDataStore.HttpCookieStore.GetAllCookies((cookies) =>
				{
					foreach (var cookie in cookies)
					{
#pragma warning disable CA1416 // Known false positive with lambda, here we can also assert the version
						WKWebsiteDataStore.DefaultDataStore.HttpCookieStore.DeleteCookie(cookie, null);
#pragma warning restore CA1416
					}
				});
			}
#endif
        }

        public bool OpenUrlCallback(Uri uri)
        {
            // If we aren't waiting on a task, don't handle the url
            if (tcsResponse?.Task?.IsCompleted ?? true)
                return false;

            try
            {
                // If we can't handle the url, don't
                if (!MAUI.WebUtils.CanHandleCallback(redirectUri, uri))
                    return false;

                currentViewController?.DismissViewControllerAsync(true);
                currentViewController = null;

                tcsResponse.TrySetResult(new WebAuthenticatorResult(uri, currentOptions?.ResponseDecoder));
                return true;
            }
            catch (Exception ex)
            {
                // TODO change this to ILogger?
                Console.WriteLine(ex);
            }
            return false;
        }

        static bool VerifyHasUrlSchemeOrDoesntRequire(string scheme)
        {
			// app is currently supporting iOS11+ so no need for these checks.
            return true;
            //// iOS11+ uses sfAuthenticationSession which handles its own url routing
            //if (OperatingSystem.IsIOSVersionAtLeast(11, 0) || OperatingSystem.IsTvOSVersionAtLeast(11, 0))
            //    return true;

            //return AppInfoImplementation.VerifyHasUrlScheme(scheme);
        }

#if IOS
		class NativeSFSafariViewControllerDelegate : SFSafariViewControllerDelegate
		{
			public Action<SFSafariViewController> DidFinishHandler { get; set; }

			public override void DidFinish(SFSafariViewController controller) =>
				DidFinishHandler?.Invoke(controller);
		}

		class ContextProvider : NSObject, IASWebAuthenticationPresentationContextProviding
		{
			public ContextProvider(UIWindow window) =>
				Window = window;

			public readonly UIWindow Window;

			[Export("presentationAnchorForWebAuthenticationSession:")]
			public UIWindow GetPresentationAnchor(ASWebAuthenticationSession session)
				=> Window;
		}
#endif
    }
}

#endif
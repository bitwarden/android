using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Microsoft.Maui.Authentication;

#if IOS
using WebAuthenticator = Bit.Core.Utilities.MAUI.WebAuthenticator;
using WebAuthenticatorResult = Bit.Core.Utilities.MAUI.WebAuthenticatorResult;
using WebAuthenticatorOptions = Bit.Core.Utilities.MAUI.WebAuthenticatorOptions;
#endif

namespace Bit.App.Pages
{
    public abstract class CaptchaProtectedViewModel : BaseViewModel
    {
        protected abstract II18nService i18nService { get; }
        protected abstract IEnvironmentService environmentService { get; }
        protected abstract IDeviceActionService deviceActionService { get; }
        protected abstract IPlatformUtilsService platformUtilsService { get; }
        protected string _captchaToken = null;

        public bool FromIosExtension { get; set; }

        protected async Task<bool> HandleCaptchaAsync(string captchaSiteKey, bool needsCaptcha, Func<Task> onSuccess)
        {
            if (!needsCaptcha)
            {
                _captchaToken = null;
                return false;
            }

            if (await HandleCaptchaAsync(captchaSiteKey))
            {
                await onSuccess();
                _captchaToken = null;
            }
            return true;
        }

        protected async Task<bool> HandleCaptchaAsync(string CaptchaSiteKey)
        {
            var callbackUri = "bitwarden://captcha-callback";
            var data = AppHelpers.EncodeDataParameter(new
            {
                siteKey = CaptchaSiteKey,
                locale = i18nService.Culture.TwoLetterISOLanguageName,
                callbackUri = callbackUri,
                captchaRequiredText = AppResources.CaptchaRequired,
            });

            var url = environmentService.GetWebVaultUrl() +
                      "/captcha-mobile-connector.html?" +
                      "data=" + data +
                      "&parent=" + Uri.EscapeDataString(callbackUri) +
                      "&v=1";

            WebAuthenticatorResult authResult = null;
            bool cancelled = false;
            try
            {
                // PrefersEphemeralWebBrowserSession should be false to allow access to the hCaptcha accessibility
                // cookie set in the default browser
                // https://www.hcaptcha.com/accessibility
                var options = new WebAuthenticatorOptions
                {
                    Url = new Uri(url),
                    CallbackUrl = new Uri(callbackUri),
                    PrefersEphemeralWebBrowserSession = false,
#if IOS
                    ShouldUseSharedApplicationKeyWindow = FromIosExtension
#endif
                };
                authResult = await WebAuthenticator.AuthenticateAsync(options);
            }
            catch (TaskCanceledException)
            {
                await deviceActionService.HideLoadingAsync();
                cancelled = true;
            }

            if (cancelled == false && authResult != null &&
                authResult.Properties.TryGetValue("token", out _captchaToken))
            {
                return true;
            }
            else
            {
                await platformUtilsService.ShowDialogAsync(AppResources.CaptchaFailed,
                    AppResources.CaptchaRequired);
                return false;
            }
        }
    }
}

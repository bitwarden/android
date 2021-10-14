using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Xamarin.Essentials;

namespace Bit.App.Pages
{
    public abstract class CaptchaProtectedViewModel : BaseViewModel
    {
        protected abstract II18nService i18nService { get; }
        protected abstract IEnvironmentService environmentService { get; }
        protected abstract IDeviceActionService deviceActionService { get; }
        protected abstract IPlatformUtilsService platformUtilsService { get; }
        protected string _captchaToken = null;

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

            var url = environmentService.GetWebVaultUrl();
            if (url == null)
            {
                url = "https://vault.bitwarden.com";
            }
            url += "/captcha-mobile-connector.html?" + "data=" + data +
                "&parent=" + Uri.EscapeDataString(callbackUri) + "&v=1";

            WebAuthenticatorResult authResult = null;
            bool cancelled = false;
            try
            {
                var options = new WebAuthenticatorOptions
                {
                    Url = new Uri(url),
                    CallbackUrl = new Uri(callbackUri),
                    PrefersEphemeralWebBrowserSession = true,
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

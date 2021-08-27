using System;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Newtonsoft.Json;
using Xamarin.Essentials;
using Xamarin.Forms;

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
            var data = EncodeDataParameter(new
            {
                siteKey = CaptchaSiteKey,
                locale = i18nService.Culture.TwoLetterISOLanguageName,
                callbackUri = callbackUri,
                captchaRequiredText = AppResources.CaptchaRequired,
            });

            var url = environmentService.GetWebVaultUrl() + "/captcha-mobile-connector.html?" + "data=" + data +
                "&parent=" + Uri.EscapeDataString(callbackUri) + "&v=1";

            WebAuthenticatorResult authResult = null;
            bool cancelled = false;
            try
            {
                var options = new WebAuthenticatorOptions()
                {
                    Url = new Uri(url),
                    CallbackUrl = new Uri(callbackUri),
                    PrefersEphemeralWebBrowserSession = true
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

        private string EncodeDataParameter(object obj)
        {
            string EncodeMultibyte(Match match)
            {
                return Convert.ToChar(Convert.ToUInt32($"0x{match.Groups[1].Value}", 16)).ToString();
            }

            var escaped = Uri.EscapeDataString(JsonConvert.SerializeObject(obj));
            var multiByteEscaped = Regex.Replace(escaped, "%([0-9A-F]{2})", EncodeMultibyte);
            return Convert.ToBase64String(Encoding.UTF8.GetBytes(multiByteEscaped));
        }

    }
}

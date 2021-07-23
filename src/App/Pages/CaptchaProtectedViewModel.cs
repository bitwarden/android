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

            var url = environmentService.WebVaultUrl + "/captcha-mobile-connector.html?" + "data=" + data +
                "&parent=" + Uri.EscapeDataString(callbackUri) + "&v=1";

            WebAuthenticatorResult authResult = null;
            bool cancelled = false;
            try
            {
                authResult = await WebAuthenticator.AuthenticateAsync(new Uri(url),
                    new Uri(callbackUri));
            }
            catch (TaskCanceledException)
            {
                await deviceActionService.HideLoadingAsync();
                cancelled = true;
            }
            catch (Exception e)
            {
                // WebAuthenticator throws NSErrorException if iOS flow is cancelled - by setting cancelled to true
                // here we maintain the appearance of a clean cancellation (we don't want to do this across the board
                // because we still want to present legitimate errors).  If/when this is fixed, we can remove this
                // particular catch block (catching taskCanceledException above must remain)
                // https://github.com/xamarin/Essentials/issues/1240
                if (Device.RuntimePlatform == Device.iOS)
                {
                    await deviceActionService.HideLoadingAsync();
                    cancelled = true;
                }
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

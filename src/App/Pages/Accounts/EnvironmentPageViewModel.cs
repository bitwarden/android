using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class EnvironmentPageViewModel : BaseViewModel
    {
        private readonly IEnvironmentService _environmentService;

        public EnvironmentPageViewModel()
        {
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");

            PageTitle = AppResources.Settings;
            BaseUrl = _environmentService.BaseUrl;
            WebVaultUrl = _environmentService.WebVaultUrl;
            ApiUrl = _environmentService.ApiUrl;
            IdentityUrl = _environmentService.IdentityUrl;
            IconsUrl = _environmentService.IconsUrl;
            NotificationsUrls = _environmentService.NotificationsUrl;
            SubmitCommand = new Command(async () => await SubmitAsync());
        }

        public Command SubmitCommand { get; }
        public string BaseUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrls { get; set; }
        public Action SubmitSuccessAction { get; set; }
        public Action CloseAction { get; set; }

        public async Task SubmitAsync()
        {
            var resUrls = await _environmentService.SetUrlsAsync(new Core.Models.Data.EnvironmentUrlData
            {
                Base = BaseUrl,
                Api = ApiUrl,
                Identity = IdentityUrl,
                WebVault = WebVaultUrl,
                Icons = IconsUrl,
                Notifications = NotificationsUrls
            });

            // re-set urls since service can change them, ex: prefixing https://
            BaseUrl = resUrls.Base;
            WebVaultUrl = resUrls.WebVault;
            ApiUrl = resUrls.Api;
            IdentityUrl = resUrls.Identity;
            IconsUrl = resUrls.Icons;
            NotificationsUrls = resUrls.Notifications;

            SubmitSuccessAction?.Invoke();
        }
    }
}

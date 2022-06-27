using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class EnvironmentPageViewModel : BaseViewModel
    {
        private readonly IEnvironmentService _environmentService;
        private bool _validForm;
        private bool _baseUrlValid;

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
            SubmitCommand = new AsyncCommand(SubmitAsync, allowsMultipleExecutions: false);
        }

        public ICommand SubmitCommand { get; }
        public string BaseUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrls { get; set; }
        public Action SubmitSuccessAction { get; set; }
        public Action CloseAction { get; set; }

        public bool BaseUrlValid { get; set; }
        public bool ApiUrlValid { get; set; }
        public bool IdentityUrlValid { get; set; }
        public bool WebVaultUrlValid { get; set; }
        public bool IconsUrlValid { get; set; }

        public async Task SubmitAsync()
        {
            if (!FormValidation())
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.EnvironmentPageUrlsError, AppResources.Ok);
                return;
            }

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

        public bool FormValidation()
        {
            BaseUrlValid = string.IsNullOrEmpty(BaseUrl) || Uri.IsWellFormedUriString(BaseUrl, UriKind.Relative);
            ApiUrlValid = string.IsNullOrEmpty(ApiUrl) || Uri.IsWellFormedUriString(BaseUrl, UriKind.Relative);
            IdentityUrlValid = string.IsNullOrEmpty(IdentityUrl) || Uri.IsWellFormedUriString(BaseUrl, UriKind.Relative);
            WebVaultUrlValid = string.IsNullOrEmpty(WebVaultUrl) || Uri.IsWellFormedUriString(BaseUrl, UriKind.Relative);
            IconsUrlValid = string.IsNullOrEmpty(IconsUrl) || Uri.IsWellFormedUriString(BaseUrl, UriKind.Relative);

            return BaseUrlValid && ApiUrlValid && IdentityUrlValid && WebVaultUrlValid && IconsUrlValid;
        }
    }
}

using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class EnvironmentPageViewModel : BaseViewModel
    {
        private readonly IEnvironmentService _environmentService;
        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        public EnvironmentPageViewModel()
        {
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");

            PageTitle = AppResources.Settings;
            BaseUrl = _environmentService.BaseUrl == EnvironmentUrlData.DefaultEU.Base || EnvironmentUrlData.DefaultUS.Base == _environmentService.BaseUrl ?
                string.Empty : _environmentService.BaseUrl;
            WebVaultUrl = _environmentService.WebVaultUrl;
            ApiUrl = _environmentService.ApiUrl;
            IdentityUrl = _environmentService.IdentityUrl;
            IconsUrl = _environmentService.IconsUrl;
            NotificationsUrls = _environmentService.NotificationsUrl;
            SubmitCommand = new AsyncCommand(SubmitAsync, onException: ex => OnSubmitException(ex), allowsMultipleExecutions: false);
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

        public async Task SubmitAsync()
        {
            if (!ValidateUrls())
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

        public bool ValidateUrls()
        {
            bool IsUrlValid(string url)
            {
                return string.IsNullOrEmpty(url) || Uri.IsWellFormedUriString(url, UriKind.RelativeOrAbsolute);
            }

            return IsUrlValid(BaseUrl)
                && IsUrlValid(ApiUrl)
                && IsUrlValid(IdentityUrl)
                && IsUrlValid(WebVaultUrl)
                && IsUrlValid(IconsUrl);
        }

        private void OnSubmitException(Exception ex)
        {
            _logger.Value.Exception(ex);
            Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok);
        }
    }
}

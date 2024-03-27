using System.Windows.Input;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using BwRegion = Bit.Core.Enums.Region;

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
            SubmitCommand = CreateDefaultAsyncRelayCommand(SubmitAsync, onException: OnSubmitException, allowsMultipleExecutions: false);
            Init();
        }

        public void Init()
        {
            if (_environmentService.SelectedRegion != BwRegion.SelfHosted ||
                _environmentService.BaseUrl == BwRegion.US.BaseUrl() ||
                _environmentService.BaseUrl == BwRegion.EU.BaseUrl())
            {
                return;
            }

            BaseUrl = _environmentService.BaseUrl;
            WebVaultUrl = _environmentService.WebVaultUrl;
            ApiUrl = _environmentService.ApiUrl;
            IdentityUrl = _environmentService.IdentityUrl;
            IconsUrl = _environmentService.IconsUrl;
            NotificationsUrls = _environmentService.NotificationsUrl;
        }

        public ICommand SubmitCommand { get; }
        public string BaseUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrls { get; set; }
        public Func<Task> SubmitSuccessTask { get; set; }
        public Action CloseAction { get; set; }

        public async Task SubmitAsync()
        {
            if (!ValidateUrls())
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.EnvironmentPageUrlsError, AppResources.Ok);
                return;
            }
            var urls = new Core.Models.Data.EnvironmentUrlData
            {
                Base = BaseUrl,
                Api = ApiUrl,
                Identity = IdentityUrl,
                WebVault = WebVaultUrl,
                Icons = IconsUrl,
                Notifications = NotificationsUrls
            };
            var resUrls = await _environmentService.SetRegionAsync(urls.Region, urls);

            // re-set urls since service can change them, ex: prefixing https://
            BaseUrl = resUrls.Base;
            WebVaultUrl = resUrls.WebVault;
            ApiUrl = resUrls.Api;
            IdentityUrl = resUrls.Identity;
            IconsUrl = resUrls.Icons;
            NotificationsUrls = resUrls.Notifications;

            if (SubmitSuccessTask != null)
            {
                await SubmitSuccessTask();
            }
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

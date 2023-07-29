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
        private readonly ICertificateService _certificateService;

        private string _certificateAlias = "";
        private string _certificateUri = "";
        private string _certificateDetails = "";
        private bool _certificateHasChanged;

        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        public EnvironmentPageViewModel()
        {
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _certificateService = ServiceContainer.Resolve<ICertificateService>("certificateService");

            PageTitle = AppResources.Settings;
            BaseUrl = _environmentService.BaseUrl == EnvironmentUrlData.DefaultEU.Base || EnvironmentUrlData.DefaultUS.Base == _environmentService.BaseUrl ?
                string.Empty : _environmentService.BaseUrl;
            WebVaultUrl = _environmentService.WebVaultUrl;
            ApiUrl = _environmentService.ApiUrl;
            IdentityUrl = _environmentService.IdentityUrl;
            IconsUrl = _environmentService.IconsUrl;
            NotificationsUrls = _environmentService.NotificationsUrl;


            SubmitCommand = new AsyncCommand(SubmitAsync, onException: ex => OnSubmitException(ex), allowsMultipleExecutions: false);
            ImportCertCommand = new AsyncCommand(ImportCertAsync, allowsMultipleExecutions: false);
            UseSystemCertCommand = new AsyncCommand(UseSystemCertAsync, allowsMultipleExecutions: false);
            RemoveCertCommand = new AsyncCommand(RemoveCertAsync, allowsMultipleExecutions: false);

            _certificateUri = _environmentService.ClientCertUri;
            BindCertDetailsAsync().FireAndForget();
        }

        public ICommand SubmitCommand { get; }
        public ICommand ImportCertCommand { get; }
        public ICommand UseSystemCertCommand { get; }
        public ICommand RemoveCertCommand { get; }

        public string BaseUrl { get; set; }
        public string ApiUrl { get; set; }
        public string IdentityUrl { get; set; }
        public string WebVaultUrl { get; set; }
        public string IconsUrl { get; set; }
        public string NotificationsUrls { get; set; }

        public string CertificateAlias
        {
            get => _certificateAlias;
            set => SetProperty(ref _certificateAlias, value);
        }
        public string CertificateDetails
        {
            get => _certificateDetails;
            set => SetProperty(ref _certificateDetails, value);
        }
        public string CertificateUri
        {
            get => _certificateUri;
            set
            {
                SetProperty(ref _certificateUri, value);
                _certificateHasChanged = true;
                BindCertDetailsAsync().FireAndForget();
            }
        }

        
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
            
            await ApplyCertChanges();

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

        public async Task ImportCertAsync()
        {
            try
            {
                CertificateUri = await _certificateService.ImportCertificateAsync();
            }
            catch (Exception ex)
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, $"Failed to import the cert!\n{ex.Message}", AppResources.Ok);
            }

        }

        public async Task RemoveCertAsync()
        {
            // Mark current certificate to be removed
            CertificateUri = null;
        }

        public async Task UseSystemCertAsync()
        {
            CertificateUri = await _certificateService.ChooseSystemCertificateAsync();
        }


        private void OnSubmitException(Exception ex)
        {
            _logger.Value.Exception(ex);
            Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok);
        }

        private async Task BindCertDetailsAsync()
        {
            try
            {
                if (CertificateUri != null)
                {
                    var cert = await _certificateService.GetCertificateAsync(CertificateUri);

                    CertificateAlias = cert.Alias;
                    CertificateDetails = cert.ToString();
                }
                else
                {
                    CertificateAlias = null;
                    CertificateDetails = null;
                }
            }
            catch (Exception ex)
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, $"Failed to read cert details!\n{ex.Message}", AppResources.Ok);
            }
        }

        private async Task ApplyCertChanges()
        {
            if (!_certificateHasChanged) return;

            await _environmentService.RemoveExistingClientCert();

            if (CertificateUri != null)
            {
                await _environmentService.SetClientCertificate(CertificateUri);
            }
        }

    }
}

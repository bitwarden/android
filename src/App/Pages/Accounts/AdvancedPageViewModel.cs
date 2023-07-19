using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class AdvancedPageViewModel: BaseViewModel
    {
        private readonly IApiService _apiService;
        private readonly ICertificateService _certificateService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IStorageService _storageService;

        private string _certificateAlias = "";
        private string _certificateUri = "";
        private string _certificateDetails = "";
        private bool _isCertificateChosen = false;

        public Action OkAction { get; set; }

        public AdvancedPageViewModel() 
        {
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _certificateService = ServiceContainer.Resolve<ICertificateService>("certificateService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");

            OkCommand = new AsyncCommand(OkAsync, allowsMultipleExecutions: false);
            ImportAndUseCertCommand = new AsyncCommand(ImportAndUseCertAsync, allowsMultipleExecutions: false);
            UseSystemCertCommand = new AsyncCommand(UseSystemCertAsync, allowsMultipleExecutions: false);
            RemoveCertCommand = new AsyncCommand(RemoveCertAsync, allowsMultipleExecutions: false);

            PageTitle = AppResources.Advanced;
            Task.Run(async () => await BindCertDetailsAsync());
        }
        

        private async Task BindCertDetailsAsync()
        {
            var certUri = await GetCertUriSafe();

            try
            {
                if (certUri != null)
                {
                    var cert = await _certificateService.GetCertificateAsync(certUri);

                    CertificateUri = certUri;
                    CertificateAlias = cert.Alias;
                    CertificateDetails = cert.ToString();
                }
                else
                {
                    CertificateUri = certUri;
                    CertificateAlias = null;
                    CertificateDetails = null;
                }
            }
            catch (Exception ex)
            {
                _deviceActionService.Toast($"Failed to read cert details!\n{ex.Message}");
            }

        }

        public ICommand OkCommand { get; }
        public ICommand ImportAndUseCertCommand { get; }
        public ICommand UseSystemCertCommand { get; }
        public ICommand RemoveCertCommand { get; }

        public Task OkAsync()
        {
            OkAction?.Invoke();
            return Task.CompletedTask;
        }

        public async Task ImportAndUseCertAsync()
        {
            try
            {
                var certUri = await _certificateService.ImportCertificateAsync();
                if (!string.IsNullOrEmpty(certUri))
                {
                    _certificateService.TryRemoveCertificate(await GetCertUriSafe());
                    await _storageService.SaveAsync(Constants.ClientAuthCertificateAliasKey, certUri);
                    await BindCertDetailsAsync();
                }
            }
            catch (Exception ex)
            {
                _deviceActionService.Toast($"Failed to import the cert!\n{ex.Message}");
            }

        }

        public async Task RemoveCertAsync()
        {
            _certificateService.TryRemoveCertificate(CertificateUri);
            await _storageService.RemoveAsync(Constants.ClientAuthCertificateAliasKey);
            await BindCertDetailsAsync();
        }

        public async Task UseSystemCertAsync()
        {
            var certUri = await _certificateService.ChooseSystemCertificateAsync();

            if (!string.IsNullOrEmpty(certUri))
            {
                _certificateService.TryRemoveCertificate(await GetCertUriSafe());

                await _storageService.SaveAsync(Constants.ClientAuthCertificateAliasKey, certUri);

                await BindCertDetailsAsync();
            }
        }

        public void ReloadClientAuthCertificate()
        {
            _apiService.ReloadClientAuthCertificateAsync().GetAwaiter().GetResult();
        }

        public string CertificateAlias { 
            get => _certificateAlias;
            set => SetProperty(ref  _certificateAlias, value);
        }

        public string CertificateDetails
        {
            get => _certificateDetails;
            set => SetProperty(ref _certificateDetails, value);
        }

        public bool IsCertificateChosen
        {
            get => _isCertificateChosen;
            set => SetProperty(ref _isCertificateChosen, value);
        }

        public string CertificateUri
        {
            get => _certificateUri;
            set {
                SetProperty(ref _certificateUri, value);
                IsCertificateChosen = !string.IsNullOrEmpty(_certificateUri);
            } 
        }

        private async Task<string> GetCertUriSafe()
        {
            try
            {
                return await _storageService.GetAsync<string>(Constants.ClientAuthCertificateAliasKey);
            }
            catch
            {
                return string.Empty;
            }
        }
    }
}

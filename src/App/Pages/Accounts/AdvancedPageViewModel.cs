using System;
using System.Collections.Generic;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Utilities;
using Bit.Core.Abstractions;
using System.Threading.Tasks;
using System.Windows.Input;
using Xamarin.CommunityToolkit.ObjectModel;
using System.Runtime.ConstrainedExecution;
using Xamarin.Forms;
using Bit.Core;
using System.Runtime.CompilerServices;

namespace Bit.App.Pages
{
    public class AdvancedPageViewModel: BaseViewModel
    {
        private readonly ICertificateService _certificateService;
        private readonly IStorageService _storageService;

        private string _certificateAlias = "";
        private string _certificateUri = "";
        private string _certificateDetails = "";
        private bool _isCertificateChosen = false;

        public Action OkAction { get; set; }

        public AdvancedPageViewModel() 
        {
            _certificateService = ServiceContainer.Resolve<ICertificateService>("certificateService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");

            OkCommand = new AsyncCommand(OkAsync, allowsMultipleExecutions: false);
            ImportAndUseCertCommand = new AsyncCommand(ImportAndUseCertAsync, allowsMultipleExecutions: false);
            UseSystemCertCommand = new AsyncCommand(UseSystemCertAsync, allowsMultipleExecutions: false);

            PageTitle = "Advanced";
            Task.Run(async () => await BindCertDetailsAsync());
        }
        
        public async Task InitAsync() 
        {
            await BindCertDetailsAsync();
        }

        private async Task BindCertDetailsAsync()
        {
            var certUri = await GetCertUriSafe();

            await PrintAsync($"->binding->{certUri}");

            try
            {
                if (certUri != null)
                {
                    var cert = await _certificateService.GetCertificateAsync(certUri);

                    CertificateUri = certUri;
                    CertificateAlias = cert.Alias;
                    CertificateDetails = cert.ToString();

                    await PrintAsync($"haha->{certUri}");
                    await PrintAsync($"haha->{CertificateAlias}");
                    await PrintAsync($"haha->{CertificateDetails}");

                    await PrintAsync($"->binding->Invokedwithuri");

                }
                else
                {
                    CertificateUri = certUri;

                    await PrintAsync($"->binding->Invoked-without-uri");
                }
            }
            catch (Exception ex)
            {
                await PrintAsync(ex.ToString());
            }

        }

        public ICommand OkCommand { get; }
        public ICommand ImportAndUseCertCommand { get; }
        public ICommand UseSystemCertCommand { get; }

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
                await PrintAsync(ex.ToString());
            }

        }

        public async Task UseSystemCertAsync()
        {
            var certUri = await _certificateService.ChooseSystemCertificateAsync();
            await PrintAsync(certUri);
            if (!string.IsNullOrEmpty(certUri))
            {
                _certificateService.TryRemoveCertificate(await GetCertUriSafe());
                await PrintAsync("removed--");

                await _storageService.SaveAsync(Constants.ClientAuthCertificateAliasKey, certUri);

                await PrintAsync("saved--");

                await BindCertDetailsAsync();

                await PrintAsync("binded--");
            }
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

        private async Task PrintAsync(string text)
        {
            await Device.InvokeOnMainThreadAsync(async () =>
            {
                Page currentPage = Xamarin.Forms.Application.Current.MainPage;
                await currentPage.DisplayAlert("TEXXXT Cert", $"{text}", "OK");
            });
        }
    }
}

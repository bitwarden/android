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

namespace Bit.App.Pages
{
    public class AdvancedPageViewModel: BaseViewModel
    {
        private readonly ICertificateService _certificateService;
        private readonly IStorageService _storageService;

        private bool _isCertificateChosen = false;
        private string _certificateAlias = "";
        private string _certificateDetails = "";

        public Action OkAction { get; set; }

        public AdvancedPageViewModel() 
        {
            _certificateService = ServiceContainer.Resolve<ICertificateService>("certificateService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            OkCommand = new AsyncCommand(OkAsync, allowsMultipleExecutions: false);
            InstallAndUseCertCommand = new AsyncCommand(InstallAndUseCertAsync, allowsMultipleExecutions: false);
            ChooseInstalledCertCommand = new AsyncCommand(ChooseInstalledCertAsync, allowsMultipleExecutions: false);

            PageTitle = "Advanced";

            BindCertificateDetails();
        }

        private void BindCertificateDetails()
        {
            Task.Run(async () => {
                
                await Task.Delay(5000);

                var cert = _certificateService.GetCertificate("aabb");
                if (cert != null)
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        IsCertificateChosen = true;
                        CertificateAlias = cert.Alias;
                        CertificateDetails = cert.ToString();
                    });
                }
                else
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        IsCertificateChosen = false;
                    });
                }
            });
        }

        public ICommand OkCommand { get; }
        public ICommand InstallAndUseCertCommand { get; }
        public ICommand ChooseInstalledCertCommand { get; }



        public Task OkAsync()
        {
            OkAction?.Invoke();
            return Task.CompletedTask;
        }

        public async Task InstallAndUseCertAsync()
        {
            var installed = await _certificateService.InstallCertificateAsync();
            if (installed)
            {
                var alias = await _certificateService.ChooseCertificateAsync();
            }
        }

        public Task ChooseInstalledCertAsync()
        {
            return Task.CompletedTask;
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
    }
}

using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ExportVaultPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly II18nService _i18nService;
        private readonly ICryptoService _cryptoService;
        private readonly IExportService _exportService;

        private int _fileFormatSelectedIndex;
        private bool _showPassword;
        private string _masterPassword;

        public ExportVaultPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _exportService = ServiceContainer.Resolve<IExportService>("exportService");

            PageTitle = AppResources.ExportVault;
            TogglePasswordCommand = new Command(TogglePassword);
            ExportVaultCommand = new Command(async () => await ExportVaultAsync());

            FileFormatOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>("json", ".json"),
                new KeyValuePair<string, string>("csv", ".csv"),
            };
        }

        public async Task InitAsync()
        {
            FileFormatSelectedIndex = FileFormatOptions.FindIndex(k => k.Key == "json");
        }

        public List<KeyValuePair<string, string>> FileFormatOptions { get; set; }

        public int FileFormatSelectedIndex
        {
            get => _fileFormatSelectedIndex;
            set
            {
                SetProperty(ref _fileFormatSelectedIndex, value);
            }
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon)
                });
        }

        public string MasterPassword
        {
            get => _masterPassword;
            set => SetProperty(ref _masterPassword, value);
        }

        public Command TogglePasswordCommand { get; }

        public string ShowPasswordIcon => ShowPassword ? "" : "";

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            (Page as ExportVaultPage).MasterPasswordEntry.Focus();
        }

        public Command ExportVaultCommand { get; }

        public async Task ExportVaultAsync()
        {
            if (string.IsNullOrEmpty(_masterPassword))
            {
                _platformUtilsService.ShowToast("error", _i18nService.T("AnErrorHasOccurred"),
                    _i18nService.T("InvalidMasterPassword"));
                return;
            }

            var keyHash = await _cryptoService.HashPasswordAsync(_masterPassword, null);
            var storedKeyHash = await _cryptoService.GetKeyHashAsync();
            if (storedKeyHash != null && keyHash != null && storedKeyHash == keyHash)
            {
                try
                {
                    // await _deviceActionService.ShowLoadingAsync(_i18nService.T("ExportingVault"));

                    var data = _exportService.GetExport(FileFormatOptions[FileFormatSelectedIndex].Key);
                    
                    System.Diagnostics.Debug.WriteLine("ExportVault format: {0} / data: {1}", 
                        FileFormatOptions[FileFormatSelectedIndex].Key, data);

                    // await _deviceActionService.HideLoadingAsync();
                }
                catch
                {
                }
            }
            else
            {
                _platformUtilsService.ShowToast("error", _i18nService.T("AnErrorHasOccurred"),
                    _i18nService.T("InvalidMasterPassword"));
            }
        }
    }
}

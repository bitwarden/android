using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
#if !FDROID
using Microsoft.AppCenter.Crashes;
#endif
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
        private byte[] _exportResult;
        private string _defaultFilename;

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
                new KeyValuePair<string, string>("csv", ".csv")
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
            set { SetProperty(ref _fileFormatSelectedIndex, value); }
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[] {nameof(ShowPasswordIcon)});
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
                await _platformUtilsService.ShowDialogAsync(_i18nService.T("InvalidMasterPassword"));
                return;
            }

            var keyHash = await _cryptoService.HashPasswordAsync(_masterPassword, null);
            MasterPassword = string.Empty;
            
            var storedKeyHash = await _cryptoService.GetKeyHashAsync();
            if (storedKeyHash != null && keyHash != null && storedKeyHash == keyHash)
            {
                try
                {
                    var data = _exportService.GetExport(FileFormatOptions[FileFormatSelectedIndex].Key);
                    var fileFormat = FileFormatOptions[FileFormatSelectedIndex].Key;
                    _defaultFilename = _exportService.GetFileName(null, fileFormat);
                    _exportResult = Encoding.ASCII.GetBytes(data.Result);

                    if (!_deviceActionService.SaveFile(_exportResult, null, _defaultFilename, null))
                    {
                        ClearResult();
                        await _platformUtilsService.ShowDialogAsync(_i18nService.T("ExportVaultFailure"));
                    }
                }
                catch (Exception ex)
                {
                    ClearResult();
                    await _platformUtilsService.ShowDialogAsync(_i18nService.T("ExportVaultFailure"));
                    System.Diagnostics.Debug.WriteLine(">>> {0}: {1}", ex.GetType(), ex.StackTrace);
#if !FDROID
                    Crashes.TrackError(ex);
#endif
                }
            }
            else
            {
                await _platformUtilsService.ShowDialogAsync(_i18nService.T("InvalidMasterPassword"));
            }
        }

        public async void SaveFileSelected(string contentUri, string filename)
        {
            if (_deviceActionService.SaveFile(_exportResult, null, filename ?? _defaultFilename, contentUri))
            {
                ClearResult();
                _platformUtilsService.ShowToast("success", null, _i18nService.T("ExportVaultSuccess"));
                return;
            }

            ClearResult();
            await _platformUtilsService.ShowDialogAsync(_i18nService.T("ExportVaultFailure"));
        }

        private void ClearResult()
        {
            _defaultFilename = null;
            _exportResult = null;
        }
    }
}

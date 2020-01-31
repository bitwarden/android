using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ExportVaultPageViewModel : BaseViewModel
    {
        //private readonly IDeviceActionService _deviceActionService;
        //private readonly IPlatformUtilsService _platformUtilsService;
        //private readonly IStorageService _storageService;
        //private readonly IStateService _stateService;

        private int _fileFormatSelectedIndex;
        private bool _showPassword;
        private string _masterPassword;

        public ExportVaultPageViewModel()
        {
            //_deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            //_platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            //_storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            //_stateService = ServiceContainer.Resolve<IStateService>("stateService");

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
            // TODO validate password first

            System.Diagnostics.Debug.WriteLine("ExportVaultAsync() formatIndex: {0} / password: {1}", FileFormatSelectedIndex, _masterPassword);
        }
    }
}

using System;
using System.Security.Cryptography.X509Certificates;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities.AccountManagement;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginApproveDeviceViewModel : BaseViewModel
    {
        private bool _rememberThisDevice;
        private bool _approveWithMyOtherDeviceEnabled;
        private bool _requestAdminApprovalEnabled;
        private bool _approveWithMasterPasswordEnabled;
        private string _email;
        private readonly IStateService _stateService;
        private readonly IApiService _apiService;
        private IDeviceTrustCryptoService _deviceTrustCryptoService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;
        private readonly IMessagingService _messagingService;

        public ICommand ApproveWithMyOtherDeviceCommand { get; }
        public ICommand RequestAdminApprovalCommand { get; }
        public ICommand ApproveWithMasterPasswordCommand { get; }
        public ICommand ContinueCommand { get; }
        public ICommand LogoutCommand { get; }

        public Action LogInWithMasterPasswordAction { get; set; }
        public Action LogInWithDeviceAction { get; set; }
        public Action RequestAdminApprovalAction { get; set; }
        public Action ContinueToVaultAction { get; set; }

        public LoginApproveDeviceViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>();
            _apiService = ServiceContainer.Resolve<IApiService>();
            _deviceTrustCryptoService = ServiceContainer.Resolve<IDeviceTrustCryptoService>();
            _authService = ServiceContainer.Resolve<IAuthService>();
            _syncService = ServiceContainer.Resolve<ISyncService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();

            PageTitle = AppResources.LogInInitiated;
            RememberThisDevice = true;

            ApproveWithMyOtherDeviceCommand = new AsyncCommand(() => SetDeviceTrustAndInvokeAsync(LogInWithDeviceAction),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            RequestAdminApprovalCommand = new AsyncCommand(() => SetDeviceTrustAndInvokeAsync(RequestAdminApprovalAction),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            ApproveWithMasterPasswordCommand = new AsyncCommand(() => SetDeviceTrustAndInvokeAsync(LogInWithMasterPasswordAction),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            ContinueCommand = new AsyncCommand(CreateNewSsoUserAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            LogoutCommand = new Command(() => _messagingService.Send(AccountsManagerMessageCommands.LOGOUT));
        }

        public string LoggingInAsText => string.Format(AppResources.LoggingInAsX, Email);

        public bool RememberThisDevice
        {
            get => _rememberThisDevice;
            set => SetProperty(ref _rememberThisDevice, value);
        }

        public bool ApproveWithMyOtherDeviceEnabled
        {
            get => _approveWithMyOtherDeviceEnabled;
            set => SetProperty(ref _approveWithMyOtherDeviceEnabled, value);
        }

        public bool RequestAdminApprovalEnabled
        {
            get => _requestAdminApprovalEnabled;
            set => SetProperty(ref _requestAdminApprovalEnabled, value,
                additionalPropertyNames: new[] { nameof(IsNewUser) });
        }

        public bool ApproveWithMasterPasswordEnabled
        {
            get => _approveWithMasterPasswordEnabled;
            set => SetProperty(ref _approveWithMasterPasswordEnabled, value,
                additionalPropertyNames: new[] { nameof(IsNewUser) });
        }

        public bool IsNewUser => !RequestAdminApprovalEnabled && !ApproveWithMasterPasswordEnabled;

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value, additionalPropertyNames:
                new string[] {
                    nameof(LoggingInAsText)
                });
        }

        public async Task InitAsync()
        {
            try
            {
                Email = await _stateService.GetActiveUserEmailAsync();
                var decryptOptions = await _stateService.GetAccountDecryptionOptions();
                RequestAdminApprovalEnabled = decryptOptions?.TrustedDeviceOption?.HasAdminApproval ?? false;
                ApproveWithMasterPasswordEnabled = decryptOptions?.HasMasterPassword ?? false;
                ApproveWithMyOtherDeviceEnabled = decryptOptions?.TrustedDeviceOption?.HasLoginApprovingDevice ?? false;
            }
            catch (Exception ex)
            {
                HandleException(ex);
            }
        }

        public async Task CreateNewSsoUserAsync()
        {
            await _authService.CreateNewSsoUserAsync(await _stateService.GetRememberedOrgIdentifierAsync());
            if (RememberThisDevice)
            {
                await _deviceTrustCryptoService.TrustDeviceAsync();
            }

            _syncService.FullSyncAsync(true).FireAndForget();
            await Device.InvokeOnMainThreadAsync(ContinueToVaultAction);
        }

        private async Task SetDeviceTrustAndInvokeAsync(Action action)
        {
            await _deviceTrustCryptoService.SetShouldTrustDeviceAsync(RememberThisDevice);
            await Device.InvokeOnMainThreadAsync(action);
        }
    }
}


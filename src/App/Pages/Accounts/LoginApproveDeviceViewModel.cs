using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Resources;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class LoginApproveDeviceViewModel : BaseViewModel
    {
        private bool _rememberThisDevice;
        private bool _approveWithMyOtherDeviceEnabled;
        private bool _requestAdminApprovalEnabled;
        private bool _approveWithMasterPasswordEnabled;

        public ICommand ApproveWithMyOtherDeviceCommand { get; }
        public ICommand RequestAdminApprovalCommand { get; }
        public ICommand ApproveWithMasterPasswordCommand { get; }

        public LoginApproveDeviceViewModel()
        {
            PageTitle = AppResources.LoggedIn;
            ApproveWithMyOtherDeviceCommand = new AsyncCommand(InitAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            RequestAdminApprovalCommand = new AsyncCommand(InitAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            ApproveWithMasterPasswordCommand = new AsyncCommand(InitAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

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
            set => SetProperty(ref _requestAdminApprovalEnabled, value);
        }

        public bool ApproveWithMasterPasswordEnabled
        {
            get => _approveWithMasterPasswordEnabled;
            set => SetProperty(ref _approveWithMasterPasswordEnabled, value);
        }

        public async Task InitAsync()
        {
            ApproveWithMyOtherDeviceEnabled = true;
            RequestAdminApprovalEnabled = true;
            ApproveWithMasterPasswordEnabled = true;
        }
    }
}


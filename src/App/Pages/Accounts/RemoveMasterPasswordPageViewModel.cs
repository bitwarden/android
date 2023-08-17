using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class RemoveMasterPasswordPageViewModel : BaseViewModel
    {
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IApiService _apiService;
        private readonly ISyncService _syncService;

        public Organization Organization;

        public RemoveMasterPasswordPageViewModel()
        {
            PageTitle = AppResources.RemoveMasterPassword;

            _keyConnectorService = ServiceContainer.Resolve<IKeyConnectorService>("keyConnectorService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

        }

        public async Task Init()
        {
            Organization = await _keyConnectorService.GetManagingOrganizationAsync();
        }

        public async Task MigrateAccount()
        {
            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);

            await _keyConnectorService.MigrateUserAsync();
            await _syncService.FullSyncAsync(true);

            await _deviceActionService.HideLoadingAsync();
        }

        public async Task LeaveOrganization()
        {
            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);

            await _apiService.PostLeaveOrganizationAsync(Organization.Id);
            await _syncService.FullSyncAsync(true);

            await _deviceActionService.HideLoadingAsync();
        }
    }
}

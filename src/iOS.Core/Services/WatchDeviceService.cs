using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Models;
using Newtonsoft.Json;
using WatchConnectivity;

namespace Bit.iOS.Core.Services
{
    public class WatchDeviceService : BaseWatchDeviceService
    {
        public WatchDeviceService(ICipherService cipherService,
            IEnvironmentService environmentService,
            IStateService stateService,
            IVaultTimeoutService vaultTimeoutService)
            : base(cipherService, environmentService, stateService, vaultTimeoutService)
        {
        }

        protected override bool CanSendData => WCSessionManager.SharedManager.IsValidSession;

        protected override Task SendDataToWatchAsync(WatchDTO watchDto)
        {
            var serializedData = JsonConvert.SerializeObject(watchDto);

            WCSessionManager.SharedManager.SendBackgroundHighPriorityMessage(new Dictionary<string, object>() { { "watchDto", serializedData } });

            return Task.CompletedTask;
        }
    }
}

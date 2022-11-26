using System;
using System.Threading.Tasks;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Models;

namespace Bit.Droid.Services
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

        protected override bool CanSendData => false;

        protected override Task SendDataToWatchAsync(WatchDTO watchDto) => throw new NotImplementedException();
    }
}


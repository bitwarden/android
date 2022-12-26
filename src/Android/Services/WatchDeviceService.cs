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

        protected override bool IsSupported => false;

        public override bool IsConnected => false;

        protected override bool CanSendData => false;

        protected override Task SendDataToWatchAsync(string serializedData) => throw new NotImplementedException();

        protected override void ConnectToWatch() => throw new NotImplementedException();
    }
}

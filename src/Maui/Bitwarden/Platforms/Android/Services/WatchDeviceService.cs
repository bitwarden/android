using System;
using System.Threading.Tasks;
using Bit.App.Services;
using Bit.Core.Abstractions;

namespace Bit.App.Droid.Services
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

        protected override Task SendDataToWatchAsync(byte[] rawData) => throw new NotImplementedException();

        protected override void ConnectToWatch() => throw new NotImplementedException();
    }
}

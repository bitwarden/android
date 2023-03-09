using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Foundation;
using WatchConnectivity;

namespace Bit.iOS.Core.Services
{
    public class WatchDeviceService : BaseWatchDeviceService
    {
        const string ACTION_MESSAGE_KEY = "actionMessage";
        const string TRIGGER_SYNC_ACTION_KEY = "triggerSync";

        private readonly ILogger _logger;

        public WatchDeviceService(ICipherService cipherService,
            IEnvironmentService environmentService,
            IStateService stateService,
            IVaultTimeoutService vaultTimeoutService,
            ILogger logger)
            : base(cipherService, environmentService, stateService, vaultTimeoutService)
        {
            _logger = logger;

            WCSessionManager.SharedManager.OnMessagedReceived += OnMessagedReceived;
        }

        public override bool IsConnected => WCSessionManager.SharedManager.IsSessionActivated;

        protected override bool CanSendData => WCSessionManager.SharedManager.IsValidSession;

        protected override bool IsSupported => WCSession.IsSupported;

        protected override Task SendDataToWatchAsync(byte[] rawData)
        {
            NSError error = null;
            // Lzfse is available on iOS 13+ but we're already constraining that by the constraint of watchOS version
            // so there's no way this will be executed on lower than iOS 13. So no condition is needed here.
            var data = NSData.FromArray(rawData).Compress(NSDataCompressionAlgorithm.Lzfse, out error);

            if (error != null)
            {
                _logger.Error("Can't compress Lzfse. Error: " + error.LocalizedDescription);
                return Task.CompletedTask;
            }

            // Add time to the key to make it change on every message sent so it's delivered faster.
            // If we use the same key then the OS may defer the delivery of the message because of
            // resources, reachability and other stuff
            var dict = new NSDictionary<NSString, NSObject>(new NSString($"watchDto-{DateTime.UtcNow.ToLongTimeString()}"), data);
            WCSessionManager.SharedManager.SendBackgroundHighPriorityMessage(dict);

            return Task.CompletedTask;
        }

        protected override void ConnectToWatch()
        {
            WCSessionManager.SharedManager.StartSession();
        }

        private void OnMessagedReceived(WCSession session, Dictionary<string, object> data)
        {
            if (data != null
                &&
                data.TryGetValue(ACTION_MESSAGE_KEY, out var action)
                &&
                action as string == TRIGGER_SYNC_ACTION_KEY)
            {
                SyncDataToWatchAsync().FireAndForget();
            }
        }
    }
}

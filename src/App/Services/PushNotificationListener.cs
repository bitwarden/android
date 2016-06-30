using PushNotification.Plugin.Abstractions;
using System.Diagnostics;
using PushNotification.Plugin;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;

namespace Bit.App.Services
{
    public class PushNotificationListener : IPushNotificationListener
    {
        private bool _showNotification;
        private readonly ISyncService _syncService;
        private readonly IDeviceApiRepository _deviceApiRepository;
        private readonly IAuthService _authService;
        private readonly IAppIdService _appIdService;

        public PushNotificationListener(
            ISyncService syncService,
            IDeviceApiRepository deviceApiRepository,
            IAuthService authService,
            IAppIdService appIdService)
        {
            _syncService = syncService;
            _deviceApiRepository = deviceApiRepository;
            _authService = authService;
            _appIdService = appIdService;
        }

        public void OnMessage(JObject values, DeviceType deviceType)
        {
            _showNotification = false;
            Debug.WriteLine("Message Arrived: {0}", JsonConvert.SerializeObject(values));

            var type = (Enums.PushType)values.GetValue("type", System.StringComparison.OrdinalIgnoreCase).ToObject<short>();
            switch(type)
            {
                case Enums.PushType.SyncCipherUpdate:
                case Enums.PushType.SyncCipherCreate:
                    var createUpdateMessage = values.ToObject<SyncCipherPushNotification>();
                    _syncService.SyncAsync(createUpdateMessage.Id);
                    break;
                case Enums.PushType.SyncFolderDelete:
                    var folderDeleteMessage = values.ToObject<SyncCipherPushNotification>();
                    _syncService.SyncDeleteFolderAsync(folderDeleteMessage.Id);
                    break;
                case Enums.PushType.SyncSiteDelete:
                    var siteDeleteMessage = values.ToObject<SyncCipherPushNotification>();
                    _syncService.SyncDeleteSiteAsync(siteDeleteMessage.Id);
                    break;
                case Enums.PushType.SyncCiphers:
                    var cipherMessage = values.ToObject<SyncCiphersPushNotification>();
                    _syncService.FullSyncAsync();
                    break;
                default:
                    break;
            }
        }

        public async void OnRegistered(string token, DeviceType deviceType)
        {
            Debug.WriteLine(string.Format("Push Notification - Device Registered - Token : {0}", token));

            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var response = await _deviceApiRepository.PutTokenAsync(_appIdService.AppId, new Models.Api.DeviceTokenRequest(token));
            if(response.Succeeded)
            {
                Debug.WriteLine("Registered device with server.");
            }
            else
            {
                Debug.WriteLine("Failed to register device.");
            }
        }

        public void OnUnregistered(DeviceType deviceType)
        {
            Debug.WriteLine("Push Notification - Device Unnregistered");
        }

        public void OnError(string message, DeviceType deviceType)
        {
            Debug.WriteLine(string.Format("Push notification error - {0}", message));
        }

        public bool ShouldShowNotification()
        {
            return _showNotification;
        }
    }
}

using PushNotification.Plugin.Abstractions;
using System.Diagnostics;
using PushNotification.Plugin;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;
using Plugin.Settings.Abstractions;
using System;

namespace Bit.App.Services
{
    public class PushNotificationListener : IPushNotificationListener
    {
        private bool _showNotification;
        private readonly ISyncService _syncService;
        private readonly IDeviceApiRepository _deviceApiRepository;
        private readonly IAuthService _authService;
        private readonly IAppIdService _appIdService;
        private readonly ISettings _settings;

        public PushNotificationListener(
            ISyncService syncService,
            IDeviceApiRepository deviceApiRepository,
            IAuthService authService,
            IAppIdService appIdService,
            ISettings settings)
        {
            _syncService = syncService;
            _deviceApiRepository = deviceApiRepository;
            _authService = authService;
            _appIdService = appIdService;
            _settings = settings;
        }

        public void OnMessage(JObject values, DeviceType deviceType)
        {
            if(values == null)
            {
                return;
            }

            _showNotification = false;
            Debug.WriteLine("Message Arrived: {0}", JsonConvert.SerializeObject(values));

            JToken token;
            if(!values.TryGetValue("type", StringComparison.OrdinalIgnoreCase, out token) || token == null)
            {
                return;
            }

            var type = (Enums.PushType)token.ToObject<short>();
            switch(type)
            {
                case Enums.PushType.SyncCipherUpdate:
                case Enums.PushType.SyncCipherCreate:
                    var createUpdateMessage = values.ToObject<SyncCipherPushNotification>();
                    _syncService.SyncAsync(createUpdateMessage.Id);
                    break;
                case Enums.PushType.SyncFolderDelete:
                    var folderDeleteMessage = values.ToObject<SyncCipherPushNotification>();
                    _syncService.SyncDeleteFolderAsync(folderDeleteMessage.Id, folderDeleteMessage.RevisionDate);
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
                _settings.AddOrUpdateValue(Constants.PushLastRegistrationDate, DateTime.UtcNow);
            }
            else
            {
                Debug.WriteLine("Failed to register device.");
            }
        }

        public async void OnUnregistered(DeviceType deviceType)
        {
            Debug.WriteLine("Push Notification - Device Unnregistered");
            _settings.Remove(Constants.PushLastRegistrationDate);
            await _deviceApiRepository.PutClearTokenAsync(_appIdService.AppId);
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

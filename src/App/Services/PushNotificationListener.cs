using PushNotification.Plugin.Abstractions;
using System.Diagnostics;
using PushNotification.Plugin;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;
using Plugin.Settings.Abstractions;
using System;
using XLabs.Ioc;

namespace Bit.App.Services
{
    public class PushNotificationListener : IPushNotificationListener
    {
        private bool _showNotification;
        private bool _resolved;
        private ISyncService _syncService;
        private IDeviceApiRepository _deviceApiRepository;
        private IAuthService _authService;
        private IAppIdService _appIdService;
        private ISettings _settings;

        private void Resolve()
        {
            if(_resolved)
            {
                return;
            }

            _syncService = Resolver.Resolve<ISyncService>();
            _deviceApiRepository = Resolver.Resolve<IDeviceApiRepository>();
            _authService = Resolver.Resolve<IAuthService>();
            _appIdService = Resolver.Resolve<IAppIdService>();
            _settings = Resolver.Resolve<ISettings>();

            _resolved = true;
        }

        public void OnMessage(JObject value, DeviceType deviceType)
        {
            Resolve();

            if(value == null)
            {
                return;
            }

            _showNotification = false;
            Debug.WriteLine("Message Arrived: {0}", JsonConvert.SerializeObject(value));

            if(!_authService.IsAuthenticated)
            {
                return;
            }

            JToken dataToken;
            if(!value.TryGetValue("data", StringComparison.OrdinalIgnoreCase, out dataToken) || dataToken == null)
            {
                return;
            }

            var data = dataToken.ToObject<PushNotificationDataPayload>();
            if(data?.Payload == null)
            {
                return;
            }

            switch(data.Type)
            {
                case Enums.PushType.SyncCipherUpdate:
                case Enums.PushType.SyncCipherCreate:
                    var cipherCreateUpdateMessage = JsonConvert.DeserializeObject<SyncCipherPushNotification>(data.Payload);
                    if(cipherCreateUpdateMessage.OrganizationId == null &&
                        cipherCreateUpdateMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    else if(cipherCreateUpdateMessage.OrganizationId != null &&
                        !_authService.BelongsToOrganization(cipherCreateUpdateMessage.OrganizationId))
                    {
                        break;
                    }
                    _syncService.SyncCipherAsync(cipherCreateUpdateMessage.Id);
                    break;
                case Enums.PushType.SyncFolderUpdate:
                case Enums.PushType.SyncFolderCreate:
                    var folderCreateUpdateMessage = JsonConvert.DeserializeObject<SyncFolderPushNotification>(data.Payload);
                    if(folderCreateUpdateMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    _syncService.SyncFolderAsync(folderCreateUpdateMessage.Id);
                    break;
                case Enums.PushType.SyncFolderDelete:
                    var folderDeleteMessage = JsonConvert.DeserializeObject<SyncFolderPushNotification>(data.Payload);
                    if(folderDeleteMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    _syncService.SyncDeleteFolderAsync(folderDeleteMessage.Id, folderDeleteMessage.RevisionDate);
                    break;
                case Enums.PushType.SyncLoginDelete:
                    var loginDeleteMessage = JsonConvert.DeserializeObject<SyncCipherPushNotification>(data.Payload);
                    if(loginDeleteMessage.OrganizationId == null &&
                        loginDeleteMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    else if(loginDeleteMessage.OrganizationId != null &&
                        !_authService.BelongsToOrganization(loginDeleteMessage.OrganizationId))
                    {
                        break;
                    }
                    _syncService.SyncDeleteLoginAsync(loginDeleteMessage.Id);
                    break;
                case Enums.PushType.SyncCiphers:
                case Enums.PushType.SyncVault:
                    var cipherMessage = JsonConvert.DeserializeObject<SyncUserPushNotification>(data.Payload);
                    if(cipherMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    _syncService.FullSyncAsync(true);
                    break;
                case Enums.PushType.SyncSettings:
                    var domainMessage = JsonConvert.DeserializeObject<SyncUserPushNotification>(data.Payload);
                    if(domainMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    _syncService.SyncSettingsAsync();
                    break;
                case Enums.PushType.SyncOrgKeys:
                    var orgKeysMessage = JsonConvert.DeserializeObject<SyncUserPushNotification>(data.Payload);
                    if(orgKeysMessage.UserId != _authService.UserId)
                    {
                        break;
                    }
                    _syncService.SyncProfileAsync();
                    break;
                default:
                    break;
            }
        }

        public async void OnRegistered(string token, DeviceType deviceType)
        {
            Resolve();

            Debug.WriteLine(string.Format("Push Notification - Device Registered - Token : {0}", token));

            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var response = await _deviceApiRepository.PutTokenAsync(_appIdService.AppId,
                new Models.Api.DeviceTokenRequest(token));
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

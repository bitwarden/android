#if !FDROID
using System.Diagnostics;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Bit.App.Abstractions;
using System;
using Xamarin.Forms;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core;
using Bit.Core.Models.Response;
using Bit.Core.Exceptions;

namespace Bit.App.Services
{
    public class PushNotificationListenerService : IPushNotificationListenerService
    {
        private bool _showNotification;
        private bool _resolved;
        private IStorageService _storageService;
        private ISyncService _syncService;
        private IUserService _userService;
        private IAppIdService _appIdService;
        private IApiService _apiService;
        private IMessagingService _messagingService;

        public async Task OnMessageAsync(JObject value, string deviceType)
        {
            Resolve();
            if (value == null)
            {
                return;
            }

            _showNotification = false;
            Debug.WriteLine("Message Arrived: {0}", JsonConvert.SerializeObject(value));

            NotificationResponse notification = null;
            if (deviceType == Device.Android)
            {
                notification = value.ToObject<NotificationResponse>();
            }
            else
            {
                if (!value.TryGetValue("data", StringComparison.OrdinalIgnoreCase, out JToken dataToken) ||
                    dataToken == null)
                {
                    return;
                }
                notification = dataToken.ToObject<NotificationResponse>();
            }

            var appId = await _appIdService.GetAppIdAsync();
            if (notification?.Payload == null || notification.ContextId == appId)
            {
                return;
            }

            var myUserId = await _userService.GetUserIdAsync();
            var isAuthenticated = await _userService.IsAuthenticatedAsync();
            switch (notification.Type)
            {
                case NotificationType.SyncCipherUpdate:
                case NotificationType.SyncCipherCreate:
                    var cipherCreateUpdateMessage = JsonConvert.DeserializeObject<SyncCipherNotification>(
                        notification.Payload);
                    if (isAuthenticated && cipherCreateUpdateMessage.UserId == myUserId)
                    {
                        await _syncService.SyncUpsertCipherAsync(cipherCreateUpdateMessage,
                            notification.Type == NotificationType.SyncCipherUpdate);
                    }
                    break;
                case NotificationType.SyncFolderUpdate:
                case NotificationType.SyncFolderCreate:
                    var folderCreateUpdateMessage = JsonConvert.DeserializeObject<SyncFolderNotification>(
                        notification.Payload);
                    if (isAuthenticated && folderCreateUpdateMessage.UserId == myUserId)
                    {
                        await _syncService.SyncUpsertFolderAsync(folderCreateUpdateMessage,
                            notification.Type == NotificationType.SyncFolderUpdate);
                    }
                    break;
                case NotificationType.SyncLoginDelete:
                case NotificationType.SyncCipherDelete:
                    var loginDeleteMessage = JsonConvert.DeserializeObject<SyncCipherNotification>(
                        notification.Payload);
                    if (isAuthenticated && loginDeleteMessage.UserId == myUserId)
                    {
                        await _syncService.SyncDeleteCipherAsync(loginDeleteMessage);
                    }
                    break;
                case NotificationType.SyncFolderDelete:
                    var folderDeleteMessage = JsonConvert.DeserializeObject<SyncFolderNotification>(
                        notification.Payload);
                    if (isAuthenticated && folderDeleteMessage.UserId == myUserId)
                    {
                        await _syncService.SyncDeleteFolderAsync(folderDeleteMessage);
                    }
                    break;
                case NotificationType.SyncCiphers:
                case NotificationType.SyncVault:
                case NotificationType.SyncSettings:
                    if (isAuthenticated)
                    {
                        await _syncService.FullSyncAsync(false);
                    }
                    break;
                case NotificationType.SyncOrgKeys:
                    if (isAuthenticated)
                    {
                        await _apiService.RefreshIdentityTokenAsync();
                        await _syncService.FullSyncAsync(true);
                    }
                    break;
                case NotificationType.LogOut:
                    if (isAuthenticated)
                    {
                        _messagingService.Send("logout");
                    }
                    break;
                default:
                    break;
            }
        }

        public async Task OnRegisteredAsync(string token, string deviceType)
        {
            Resolve();
            Debug.WriteLine(string.Format("Push Notification - Device Registered - Token : {0}", token));
            var isAuthenticated = await _userService.IsAuthenticatedAsync();
            if (!isAuthenticated)
            {
                return;
            }

            var appId = await _appIdService.GetAppIdAsync();
            try
            {
                await _apiService.PutDeviceTokenAsync(appId,
                    new Core.Models.Request.DeviceTokenRequest { PushToken = token });
                Debug.WriteLine("Registered device with server.");
                await _storageService.SaveAsync(Constants.PushLastRegistrationDateKey, DateTime.UtcNow);
                if (deviceType == Device.Android)
                {
                    await _storageService.SaveAsync(Constants.PushCurrentTokenKey, token);
                }
            }
            catch (ApiException)
            {
                Debug.WriteLine("Failed to register device.");
            }
        }

        public void OnUnregistered(string deviceType)
        {
            Debug.WriteLine("Push Notification - Device Unnregistered");
        }

        public void OnError(string message, string deviceType)
        {
            Debug.WriteLine(string.Format("Push notification error - {0}", message));
        }

        public bool ShouldShowNotification()
        {
            return _showNotification;
        }

        private void Resolve()
        {
            if (_resolved)
            {
                return;
            }
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _appIdService = ServiceContainer.Resolve<IAppIdService>("appIdService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _resolved = true;
        }
    }
}
#endif

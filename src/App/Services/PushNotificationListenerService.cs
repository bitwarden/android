#if !FDROID
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Response;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class PushNotificationListenerService : IPushNotificationListenerService
    {
        const string TAG = "##PUSH NOTIFICATIONS";

        private bool _showNotification;
        private LazyResolve<ISyncService> _syncService = new LazyResolve<ISyncService>();
        private LazyResolve<IStateService> _stateService = new LazyResolve<IStateService>();
        private LazyResolve<IAppIdService> _appIdService = new LazyResolve<IAppIdService>();
        private LazyResolve<IApiService> _apiService = new LazyResolve<IApiService>();
        private LazyResolve<IMessagingService> _messagingService = new LazyResolve<IMessagingService>();
        private LazyResolve<IPushNotificationService> _pushNotificationService = new LazyResolve<IPushNotificationService>();
        private LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        public async Task OnMessageAsync(JObject value, string deviceType)
        {
            Debug.WriteLine($"{TAG} OnMessageAsync called");

            if (value == null)
            {
                return;
            }

            _showNotification = false;
            Debug.WriteLine($"{TAG} Message Arrived: {JsonConvert.SerializeObject(value)}");

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

            Debug.WriteLine($"{TAG} - Notification object created: t:{notification?.Type} - p:{notification?.Payload}");

            var appId = await _appIdService.Value.GetAppIdAsync();
            if (notification?.Payload == null || notification.ContextId == appId)
            {
                return;
            }

            var myUserId = await _stateService.Value.GetActiveUserIdAsync();
            var isAuthenticated = await _stateService.Value.IsAuthenticatedAsync();
            switch (notification.Type)
            {
                case NotificationType.SyncCipherUpdate:
                case NotificationType.SyncCipherCreate:
                    var cipherCreateUpdateMessage = JsonConvert.DeserializeObject<SyncCipherNotification>(
                        notification.Payload);
                    if (isAuthenticated && cipherCreateUpdateMessage.UserId == myUserId)
                    {
                        await _syncService.Value.SyncUpsertCipherAsync(cipherCreateUpdateMessage,
                            notification.Type == NotificationType.SyncCipherUpdate);
                    }
                    break;
                case NotificationType.SyncFolderUpdate:
                case NotificationType.SyncFolderCreate:
                    var folderCreateUpdateMessage = JsonConvert.DeserializeObject<SyncFolderNotification>(
                        notification.Payload);
                    if (isAuthenticated && folderCreateUpdateMessage.UserId == myUserId)
                    {
                        await _syncService.Value.SyncUpsertFolderAsync(folderCreateUpdateMessage,
                            notification.Type == NotificationType.SyncFolderUpdate);
                    }
                    break;
                case NotificationType.SyncLoginDelete:
                case NotificationType.SyncCipherDelete:
                    var loginDeleteMessage = JsonConvert.DeserializeObject<SyncCipherNotification>(
                        notification.Payload);
                    if (isAuthenticated && loginDeleteMessage.UserId == myUserId)
                    {
                        await _syncService.Value.SyncDeleteCipherAsync(loginDeleteMessage);
                    }
                    break;
                case NotificationType.SyncFolderDelete:
                    var folderDeleteMessage = JsonConvert.DeserializeObject<SyncFolderNotification>(
                        notification.Payload);
                    if (isAuthenticated && folderDeleteMessage.UserId == myUserId)
                    {
                        await _syncService.Value.SyncDeleteFolderAsync(folderDeleteMessage);
                    }
                    break;
                case NotificationType.SyncCiphers:
                case NotificationType.SyncVault:
                case NotificationType.SyncSettings:
                    if (isAuthenticated)
                    {
                        await _syncService.Value.FullSyncAsync(false);
                    }
                    break;
                case NotificationType.SyncOrgKeys:
                    if (isAuthenticated)
                    {
                        await _apiService.Value.RefreshIdentityTokenAsync();
                        await _syncService.Value.FullSyncAsync(true);
                    }
                    break;
                case NotificationType.LogOut:
                    if (isAuthenticated)
                    {
                        _messagingService.Value.Send("logout");
                    }
                    break;
                case NotificationType.AuthRequest:
                    var passwordlessLoginMessage = JsonConvert.DeserializeObject<PasswordlessRequestNotification>(notification.Payload);

                    // if the user has not enabled passwordless logins ignore requests
                    if (!await _stateService.Value.GetApprovePasswordlessLoginsAsync(passwordlessLoginMessage?.UserId))
                    {
                        return;
                    }

                    // if there is a request modal opened ignore all incoming requests
                    if (App.Current.MainPage.Navigation.ModalStack.Any(p => p is NavigationPage navPage && navPage.CurrentPage is LoginPasswordlessPage))
                    {
                        return;
                    }

                    await _stateService.Value.SetPasswordlessLoginNotificationAsync(passwordlessLoginMessage);
                    var userEmail = await _stateService.Value.GetEmailAsync(passwordlessLoginMessage?.UserId);

                    var notificationData = new PasswordlessNotificationData()
                    {
                        Id = Constants.PasswordlessNotificationId,
                        TimeoutInMinutes = Constants.PasswordlessNotificationTimeoutInMinutes,
                        UserEmail = userEmail,
                    };

                    _pushNotificationService.Value.SendLocalNotification(AppResources.LogInRequested, String.Format(AppResources.ConfimLogInAttempForX, userEmail), notificationData);
                    _messagingService.Value.Send(Constants.PasswordlessLoginRequestKey, passwordlessLoginMessage);
                    break;
                default:
                    break;
            }
        }

        public async Task OnRegisteredAsync(string token, string deviceType)
        {
            Debug.WriteLine($"{TAG} - Device Registered - Token : {token}");
            var isAuthenticated = await _stateService.Value.IsAuthenticatedAsync();
            if (!isAuthenticated)
            {
                Debug.WriteLine($"{TAG} - not auth");
                return;
            }

            var appId = await _appIdService.Value.GetAppIdAsync();
            try
            {
#if DEBUG
                await _stateService.Value.SetPushInstallationRegistrationErrorAsync(null);
#endif

                await _apiService.Value.PutDeviceTokenAsync(appId,
                    new Core.Models.Request.DeviceTokenRequest { PushToken = token });

                Debug.WriteLine($"{TAG} Registered device with server.");

                await _stateService.Value.SetPushLastRegistrationDateAsync(DateTime.UtcNow);
                if (deviceType == Device.Android)
                {
                    await _stateService.Value.SetPushCurrentTokenAsync(token);
                }
            }
#if DEBUG
            catch (ApiException apiEx)
            {
                Debug.WriteLine($"{TAG} Failed to register device.");

                await _stateService.Value.SetPushInstallationRegistrationErrorAsync(apiEx.Error?.Message);
            }
            catch (Exception e)
            {
                await _stateService.Value.SetPushInstallationRegistrationErrorAsync(e.Message);
                throw;
            }
#else
            catch (ApiException)
            {
            }
#endif
        }

        public void OnUnregistered(string deviceType)
        {
            Debug.WriteLine($"{TAG} - Device Unnregistered");
        }

        public void OnError(string message, string deviceType)
        {
            Debug.WriteLine($"{TAG} error - {message}");
        }

        public async Task OnNotificationTapped(BaseNotificationData data)
        {
            try
            {
                if (data is PasswordlessNotificationData passwordlessNotificationData)
                {
                    var notificationUserId = await _stateService.Value.GetUserIdAsync(passwordlessNotificationData.UserEmail);
                    var notificationSaved = await _stateService.Value.GetPasswordlessLoginNotificationAsync();
                    if (notificationUserId != null && notificationSaved != null)
                    {
                        await _stateService.Value.SetActiveUserAsync(notificationUserId);
                        _messagingService.Value.Send(AccountsManagerMessageCommands.SWITCHED_ACCOUNT);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }

        public async Task OnNotificationDismissed(BaseNotificationData data)
        {
            try
            {
                if (data is PasswordlessNotificationData passwordlessNotificationData)
                {
                    var savedNotification = await _stateService.Value.GetPasswordlessLoginNotificationAsync();
                    if (savedNotification != null)
                    {
                        await _stateService.Value.SetPasswordlessLoginNotificationAsync(null);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }

        public bool ShouldShowNotification()
        {
            return _showNotification;
        }
    }
}
#endif

#if !FDROID
using System;
using System.Threading.Tasks;
using AndroidX.Core.App;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Xamarin.Forms;

namespace Bit.Droid.Services
{
    public class AndroidPushNotificationService : IPushNotificationService
    {
        private readonly IStateService _stateService;
        private readonly IPushNotificationListenerService _pushNotificationListenerService;

        public AndroidPushNotificationService(
            IStateService stateService,
            IPushNotificationListenerService pushNotificationListenerService)
        {
            _stateService = stateService;
            _pushNotificationListenerService = pushNotificationListenerService;
        }

        public bool IsRegisteredForPush => NotificationManagerCompat.From(Android.App.Application.Context)?.AreNotificationsEnabled() ?? false;

        public async Task<string> GetTokenAsync()
        {
            return await _stateService.GetPushCurrentTokenAsync();
        }

        public async Task RegisterAsync()
        {
            var registeredToken = await _stateService.GetPushRegisteredTokenAsync();
            var currentToken = await GetTokenAsync();
            if (!string.IsNullOrWhiteSpace(registeredToken) && registeredToken != currentToken)
            {
                await _pushNotificationListenerService.OnRegisteredAsync(registeredToken, Device.Android);
            }
            else
            {
                await _stateService.SetPushLastRegistrationDateAsync(DateTime.UtcNow);
            }
        }

        public Task UnregisterAsync()
        {
            // Do we ever need to unregister?
            return Task.FromResult(0);
        }
    }
}
#endif

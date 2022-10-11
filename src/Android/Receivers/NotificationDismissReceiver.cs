using Android.Content;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Services;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using static AndroidX.Concurrent.Futures.CallbackToFutureAdapter;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = "com.x8bit.bitwarden.NotificationDismissReceiver", Exported = false)]
    public class NotificationDismissReceiver : BroadcastReceiver
    {
        private bool _resolved;
        private IPushNotificationListenerService _pushNotificationListenerService;
        private ILogger _logger;

        public override void OnReceive(Context context, Intent intent)
        {
            try
            {
                Resolve();
                if (intent?.GetStringExtra(Constants.NotificationData) is string notificationDataJson)
                {
                    var notificationType = JToken.Parse(notificationDataJson).SelectToken(Constants.NotificationDataType);
                    if (notificationType.ToString() == PasswordlessNotificationData.TYPE)
                    {
                        _pushNotificationListenerService.OnNotificationDismissed(JsonConvert.DeserializeObject<PasswordlessNotificationData>(notificationDataJson)).FireAndForget();
                    }
                }
            }
            catch (System.Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private void Resolve()
        {
            if (_resolved)
            {
                return;
            }
            _pushNotificationListenerService = ServiceContainer.Resolve<IPushNotificationListenerService>();
            _logger = ServiceContainer.Resolve<ILogger>();
            _resolved = true;
        }
    }
}


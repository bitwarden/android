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
using CoreConstants = Bit.Core.Constants;

namespace Bit.Droid.Receivers
{
    [BroadcastReceiver(Name = Constants.PACKAGE_NAME + "." + nameof(NotificationDismissReceiver), Exported = false)]
    public class NotificationDismissReceiver : BroadcastReceiver
    {
        private readonly LazyResolve<IPushNotificationListenerService> _pushNotificationListenerService = new LazyResolve<IPushNotificationListenerService>();
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        public override void OnReceive(Context context, Intent intent)
        {
            try
            {
                if (intent?.GetStringExtra(CoreConstants.NotificationData) is string notificationDataJson)
                {
                    var notificationType = JToken.Parse(notificationDataJson).SelectToken(CoreConstants.NotificationDataType);
                    if (notificationType.ToString() == PasswordlessNotificationData.TYPE)
                    {
                        _pushNotificationListenerService.Value.OnNotificationDismissed(JsonConvert.DeserializeObject<PasswordlessNotificationData>(notificationDataJson)).FireAndForget();
                    }
                }
            }
            catch (System.Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }
    }
}


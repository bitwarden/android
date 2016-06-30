using Bit.App.Enums;

namespace Bit.App.Models
{
    public class PushNotification
    {
        public PushType Type { get; set; }
    }

    public class SyncPushNotification : PushNotification
    {
        public string UserId { get; set; }
    }

    public class SyncCipherPushNotification : SyncPushNotification
    {
        public string CipherId { get; set; }
    }
}

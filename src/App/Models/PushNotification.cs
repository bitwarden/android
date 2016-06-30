using System;
using Bit.App.Enums;

namespace Bit.App.Models
{
    public class PushNotification
    {
        public PushType Type { get; set; }
    }

    public abstract class SyncPushNotification : PushNotification
    {
        public string UserId { get; set; }
    }

    public class SyncCipherPushNotification : SyncPushNotification
    {
        public string Id { get; set; }
        public DateTime RevisionDate { get; set; }
    }

    public class SyncCiphersPushNotification : SyncPushNotification
    {
        public DateTime Date { get; set; }
    }
}

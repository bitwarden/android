using System;
using Bit.App.Enums;
using Newtonsoft.Json.Linq;

namespace Bit.App.Models
{
    public class PushNotificationData
    {
        public PushType Type { get; set; }
    }

    public class PushNotificationDataPayload : PushNotificationData
    {
        public string Payload { get; set; }
    }

    public class SyncCipherPushNotification
    {
        public string Id { get; set; }
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public DateTime RevisionDate { get; set; }
    }

    public class SyncFolderPushNotification
    {
        public string Id { get; set; }
        public string UserId { get; set; }
        public DateTime RevisionDate { get; set; }
    }

    public class SyncUserPushNotification
    {
        public string UserId { get; set; }
        public DateTime Date { get; set; }
    }
}

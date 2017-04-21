using System;
using Bit.App.Enums;

namespace Bit.App.Models
{
    public class PushNotification
    {
        public PushType Type { get; set; }
    }

    public class SyncCipherPushNotification : PushNotification
    {
        public string Id { get; set; }
        public string UserId { get; set; }
        public string OrganizationId { get; set; }
        public DateTime RevisionDate { get; set; }
    }

    public class SyncFolderPushNotification : PushNotification
    {
        public string Id { get; set; }
        public string UserId { get; set; }
        public DateTime RevisionDate { get; set; }
    }

    public class SyncUserPushNotification : PushNotification
    {
        public string UserId { get; set; }
        public DateTime Date { get; set; }
    }
}

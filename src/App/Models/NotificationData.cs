using System;
namespace Bit.App.Models
{
    public abstract class BaseNotificationData
    {
        public abstract string NotificationType { get; }

        public string NotificationId { get; set; }
    }

    public class PasswordlessNotificationData : BaseNotificationData
    {
        public const string TYPE = "passwordlessNotificationData";

        public override string NotificationType => TYPE;

        public int TimeoutInMinutes { get; set; }

        public string UserEmail { get; set; }
    }

}


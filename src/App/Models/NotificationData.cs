using System;
namespace Bit.App.Models
{
    public abstract class BaseNotificationData
    {
        public abstract string Type { get; }

        public string Id { get; set; }
    }

    public class PasswordlessNotificationData : BaseNotificationData
    {
        public const string TYPE = "passwordlessNotificationData";

        public override string Type => TYPE;

        public int TimeoutInMinutes { get; set; }

        public string UserEmail { get; set; }
    }

}


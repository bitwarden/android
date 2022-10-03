using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationService
    {
        bool IsRegisteredForPush { get; }
        Task<bool> AreNotificationsSettingsEnabledAsync();
        Task<string> GetTokenAsync();
        Task RegisterAsync();
        Task UnregisterAsync();
        void SendLocalNotification(string title, string message, string notificationId, string notificationType, Dictionary<string, string> notificationData = null, int notificationTimeoutMinutes = 0);
        void DismissLocalNotification(string notificationId);
    }
}

using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;

namespace Bit.App.Abstractions
{
    public interface IPushNotificationService
    {
        bool IsRegisteredForPush { get; }
        Task<bool> AreNotificationsSettingsEnabledAsync();
        Task<string> GetTokenAsync();
        Task RegisterAsync();
        Task UnregisterAsync();
        void SendLocalNotification(string title, string message, BaseNotificationData data);
        void DismissLocalNotification(string notificationId);
    }
}

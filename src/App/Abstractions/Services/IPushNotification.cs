namespace Bit.App.Abstractions
{
    public interface IPushNotificationService
    {
        string Token { get; }
        void Register();
        void Unregister();
    }
}

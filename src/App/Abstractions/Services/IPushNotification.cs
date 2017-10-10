namespace Bit.App.Abstractions
{
    public interface IPushNotification
    {
        string Token { get; }
        void Register();
        void Unregister();
    }
}

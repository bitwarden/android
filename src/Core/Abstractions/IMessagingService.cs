namespace Bit.Core.Abstractions
{
    public interface IMessagingService
    {
        void Send(string subscriber, object arg = null);
    }
}

namespace Bit.Core.Abstractions
{
    public interface IMessagingService
    {
        void Send<T>(string subscriber, T arg = default(T));
    }
}
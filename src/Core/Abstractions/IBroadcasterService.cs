using System;

namespace Bit.Core.Abstractions
{
    public interface IBroadcasterService
    {
        void Send<T>(T message, string id = null);
        void Subscribe<T>(string id, Action<T> messageCallback);
        void Unsubscribe(string id);
    }
}
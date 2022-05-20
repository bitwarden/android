using System;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IBroadcasterService
    {
        void Send(Message message);
        void Send(Message message, string id);
        void Subscribe(string id, Action<Message> messageCallback);
        void Unsubscribe(string id);
    }
}

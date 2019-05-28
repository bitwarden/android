using Bit.Core.Models.Domain;
using System;

namespace Bit.Core.Abstractions
{
    public interface IBroadcasterService
    {
        void Send(Message message, string id = null);
        void Subscribe(string id, Action<Message> messageCallback);
        void Unsubscribe(string id);
    }
}
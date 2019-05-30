using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Services
{
    public class BroadcasterService : IBroadcasterService
    {
        private readonly Dictionary<string, Action<Message>> _subscribers = new Dictionary<string, Action<Message>>();

        public void Send(Message message, string id = null)
        {
            if(!string.IsNullOrWhiteSpace(id))
            {
                if(_subscribers.ContainsKey(id))
                {
                    Task.Run(() => _subscribers[id].Invoke(message));
                }
                return;
            }
            foreach(var sub in _subscribers)
            {
                Task.Run(() => sub.Value.Invoke(message));
            }
        }

        public void Subscribe(string id, Action<Message> messageCallback)
        {
            if(_subscribers.ContainsKey(id))
            {
                return;
            }
            _subscribers.Add(id, messageCallback);
        }

        public void Unsubscribe(string id)
        {
            if(_subscribers.ContainsKey(id))
            {
                _subscribers.Remove(id);
            }
        }
    }
}

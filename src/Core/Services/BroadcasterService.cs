using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;

namespace Bit.App.Services
{
    public class BroadcasterService : IBroadcasterService
    {
        private readonly ILogger _logger;
        private readonly ConcurrentDictionary<string, Action<Message>> _subscribers = new ConcurrentDictionary<string, Action<Message>>();

        public BroadcasterService(ILogger logger)
        {
            _logger = logger;
        }

        public void Send(Message message)
        {
            foreach (var sub in _subscribers)
            {
                Task.Run(() =>
                {
                    try
                    {
                        sub.Value(message);
                    }
                    catch (Exception ex)
                    {
                        _logger.Exception(ex);
                    }
                });
            }
        }

        public void Send(Message message, string id)
        {
            if (string.IsNullOrWhiteSpace(id))
            {
                return;
            }

            if (_subscribers.TryGetValue(id, out var action))
            {
                Task.Run(() =>
                {
                    try
                    {
                        action(message);
                    }
                    catch (Exception ex)
                    {
                        _logger.Exception(ex);
                    }
                });
            }
        }

        public void Subscribe(string id, Action<Message> messageCallback)
        {
            _subscribers[id] = messageCallback;
        }

        public void Unsubscribe(string id)
        {
            _subscribers.TryRemove(id, out _);
        }

        public void Once(Action<Message> messageCallback) { }
    }
}

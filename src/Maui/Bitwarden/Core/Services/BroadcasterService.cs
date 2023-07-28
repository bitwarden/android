using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;

namespace Bit.App.Services
{
    public class BroadcasterService : IBroadcasterService
    {
        private readonly ILogger _logger;
        private readonly Dictionary<string, Action<Message>> _subscribers = new Dictionary<string, Action<Message>>();
        private object _myLock = new object();

        public BroadcasterService(ILogger logger)
        {
            _logger = logger;
        }

        public void Send(Message message)
        {
            lock (_myLock)
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
        }

        public void Send(Message message, string id)
        {
            if (string.IsNullOrWhiteSpace(id))
            {
                return;
            }

            lock (_myLock)
            {
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
        }

        public void Subscribe(string id, Action<Message> messageCallback)
        {
            lock (_myLock)
            {
                _subscribers[id] = messageCallback;
            }
        }

        public void Unsubscribe(string id)
        {
            lock (_myLock)
            {
                if (_subscribers.ContainsKey(id))
                {
                    _subscribers.Remove(id);
                }
            }
        }
    }
}

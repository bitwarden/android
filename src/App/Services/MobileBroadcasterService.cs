using Bit.Core.Abstractions;
using System;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class MobileBroadcasterService : IBroadcasterService
    {
        public void Send<T>(T message, string id = null)
        {
            if(string.IsNullOrWhiteSpace(id))
            {
                throw new NotSupportedException("Cannot send a message to all subscribers.");
            }
            MessagingCenter.Send(Application.Current, id, message);
        }

        public void Subscribe<T>(string id, Action<T> messageCallback)
        {
            MessagingCenter.Subscribe<Application, T>(Application.Current, id,
                (sender, message) => messageCallback(message));
        }

        public void Unsubscribe(string id)
        {
            MessagingCenter.Unsubscribe<Application, object>(Application.Current, id);
        }
    }
}

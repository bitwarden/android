using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class MobileMessagingService : IMessagingService
    {
        public void Send<T>(string subscriber, T arg = default(T))
        {
            Xamarin.Forms.MessagingCenter.Send(Xamarin.Forms.Application.Current, subscriber, arg);
        }
    }
}

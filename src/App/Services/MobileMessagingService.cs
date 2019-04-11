using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class MobileMessagingService : IMessagingService
    {
        public void Send(string subscriber, object arg = null)
        {
            Xamarin.Forms.MessagingCenter.Send(Xamarin.Forms.Application.Current, subscriber, arg);
        }
    }
}

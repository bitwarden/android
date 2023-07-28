using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;

namespace Bit.App.Services
{
    public class MobileBroadcasterMessagingService : IMessagingService
    {
        private readonly IBroadcasterService _broadcasterService;

        public MobileBroadcasterMessagingService(IBroadcasterService broadcasterService)
        {
            _broadcasterService = broadcasterService;
        }

        public void Send(string subscriber, object arg = null)
        {
            var message = new Message { Command = subscriber, Data = arg };
            _broadcasterService.Send(message);
        }
    }
}

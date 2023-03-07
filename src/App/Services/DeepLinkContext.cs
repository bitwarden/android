using System;
using Bit.App.Abstractions;
using Bit.Core;
using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class DeepLinkContext : IDeepLinkContext
    {
        public const string NEW_OTP_MESSAGE = "handleOTPUriMessage";

        private readonly IMessagingService _messagingService;

        public DeepLinkContext(IMessagingService messagingService)
        {
            _messagingService = messagingService;
        }

        public bool OnNewUri(Uri uri)
        {
            if (uri.Scheme == Constants.OtpAuthScheme)
            {
                _messagingService.Send(NEW_OTP_MESSAGE, uri.AbsoluteUri);
                return true;
            }

            return false;
        }
    }
}

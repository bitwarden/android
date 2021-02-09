using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class SendViewCellViewModel : ExtendedViewModel
    {
        private SendView _send;

        public SendView Send
        {
            get => _send;
            set => SetProperty(ref _send, value);
        }
    }
}

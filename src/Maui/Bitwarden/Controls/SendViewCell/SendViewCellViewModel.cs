using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class SendViewCellViewModel : ExtendedViewModel
    {
        private SendView _send;
        private bool _showOptions;

        public SendViewCellViewModel(SendView sendView, bool showOptions)
        {
            Send = sendView;
            ShowOptions = showOptions;
        }

        public SendView Send
        {
            get => _send;
            set => SetProperty(ref _send, value);
        }

        public bool ShowOptions
        {
            get => _showOptions;
            set => SetProperty(ref _showOptions, value);
        }
    }
}

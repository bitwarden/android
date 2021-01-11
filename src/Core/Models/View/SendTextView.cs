using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class SendTextView : View
    {
        public SendTextView() { }
        public SendTextView(SendText text)
        {
            Hidden = text.Hidden;
        }

        public string Text { get; set; } = null;
        public bool Hidden { get; set; }
        public string MaskedText => Text != null ? "••••••••" : null;
    }
}

using Microsoft.Maui.Controls.Platform;
using UIKit;

namespace Bit.iOS.Core.Effects
{
    public class NoEmojiKeyboardEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Element != null && Control is UITextField textField)
            {
                textField.KeyboardType = UIKeyboardType.ASCIICapable;
            }
        }

        protected override void OnDetached()
        {
        }
    }
}

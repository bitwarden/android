using Bit.Droid.Effects;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportEffect(typeof(NoEmojiKeyboardEffect), "NoEmojiKeyboardEffect")]
namespace Bit.Droid.Effects
{
    public class NoEmojiKeyboardEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Element is Entry && Control is FormsEditText editText)
            {
                editText.ImeOptions = Android.Views.InputMethods.ImeAction.Done;
                editText.InputType = Android.Text.InputTypes.ClassText | Android.Text.InputTypes.TextVariationVisiblePassword | Android.Text.InputTypes.TextFlagMultiLine;
            }
        }

        protected override void OnDetached()
        {
        }
    }
}


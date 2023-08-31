using Android.Graphics;
using Android.Text;
using Android.Views.InputMethods;
using Android.Widget;
using Bit.App.Droid.Utilities;
using Microsoft.Maui.Platform;

namespace Bit.App.Handlers
{
    public partial class EntryHandlerMappings
    {
        partial void SetupPlatform()
        {
            Microsoft.Maui.Handlers.EntryHandler.Mapper.AppendToMapping("CustomEntryHandler", (handler, entry) =>
            {
                handler.PlatformView.SetPadding(handler.PlatformView.PaddingLeft, handler.PlatformView.PaddingTop - 10, handler.PlatformView.PaddingRight,
                    handler.PlatformView.PaddingBottom + 20);
                handler.PlatformView.ImeOptions = handler.PlatformView.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                    (ImeAction)ImeFlags.NoExtractUi;
            });

            Microsoft.Maui.Handlers.EntryHandler.Mapper.AppendToMapping(nameof(IEntry.TextColor), (handler, entry) =>
            {
                handler.PlatformView.BackgroundTintList = ThemeHelpers.GetStateFocusedColors();
            });

            Microsoft.Maui.Handlers.EntryHandler.Mapper.AppendToMapping(nameof(IEntry.IsPassword), (handler, entry) =>
            {
                // Check if field type is text, otherwise ignore (numeric passwords, etc.)
                handler.PlatformView.InputType = entry.Keyboard.ToInputType();
                bool isText = (handler.PlatformView.InputType & InputTypes.ClassText) == InputTypes.ClassText,
                    isNumber = (handler.PlatformView.InputType & InputTypes.ClassNumber) == InputTypes.ClassNumber;
                if (!isText && !isNumber)
                {
                    return;
                }

                if (entry.IsPassword)
                {
                    // Element is a password field, set inputType to TextVariationPassword which disables
                    // predictive text by default
                    handler.PlatformView.InputType = handler.PlatformView.InputType |
                        (isText ? InputTypes.TextVariationPassword : InputTypes.NumberVariationPassword);
                }
                else
                {
                    // Element is not a password field, set inputType to TextVariationVisiblePassword to
                    // disable predictive text while still displaying the content.
                    handler.PlatformView.InputType = handler.PlatformView.InputType |
                        (isText ? InputTypes.TextVariationVisiblePassword : InputTypes.NumberVariationNormal);
                }

                // The workaround above forces a reset of the style properties, so we need to re-apply the font.
                // see https://xamarin.github.io/bugzilla-archives/33/33666/bug.html
                var typeface = Typeface.CreateFromAsset(handler.PlatformView.Context.Assets, "RobotoMono_Regular.ttf");
                if (handler.PlatformView is TextView label)
                {
                    label.Typeface = typeface;
                }
            });
        }
    }
}

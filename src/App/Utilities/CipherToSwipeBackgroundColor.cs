using Bit.Core.Models.View;
using Xamarin.CommunityToolkit.Converters;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class CipherToSwipeBackgroundColor : BaseConverter<CipherView, Color>
    {
        public override Color ConvertFrom(CipherView value)
        {
            if (value is null)
            {
                return Color.Default;
            }

            Color GetDisabledColor()
            {
                if (App.Current.Resources.TryGetValue("ButtonBackgroundColorDisabled", out var disabledColor))
                {
                    return (Color)disabledColor;
                }

                return Color.LightGray;
            }

            if (value.Type == Core.Enums.CipherType.Login
                &&
                value.Login?.Password is null)
            {
                return GetDisabledColor();
            }

            if (value.Type == Core.Enums.CipherType.Card
                &&
                value.Card?.Number is null)
            {
                return GetDisabledColor();
            }

            if (value.Type == Core.Enums.CipherType.SecureNote
                &&
                value.Notes is null)
            {
                return GetDisabledColor();
            }

            if (App.Current.Resources.TryGetValue("PrimaryColor", out var enabledColor))
            {
                return (Color)enabledColor;
            }
            return Color.Default;
        }

        public override CipherView ConvertBackTo(Color value) => throw new System.NotImplementedException();
    }
}

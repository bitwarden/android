using Refractored.FabControl;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class Fab : FloatingActionButtonView
    {
        public Fab(FabLayout fabLayout, string icon, Action<object, EventArgs> clickedAction)
        {
            ImageName = icon;
            ColorNormal = Color.FromHex("3c8dbc");
            ColorPressed = Color.FromHex("3883af");
            ColorRipple = Color.FromHex("3883af");
            Clicked = clickedAction;

            AbsoluteLayout.SetLayoutFlags(this, AbsoluteLayoutFlags.PositionProportional);
            AbsoluteLayout.SetLayoutBounds(this, new Rectangle(1, 1, AbsoluteLayout.AutoSize,
                AbsoluteLayout.AutoSize));

            fabLayout.Children.Add(this);
        }
    }
}

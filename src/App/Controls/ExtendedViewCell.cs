using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedViewCell : ViewCell
    {
        public static readonly BindableProperty BackgroundColorProperty =
            BindableProperty.Create(nameof(BackgroundColor), typeof(Color), typeof(ExtendedTextCell), Color.White);

        public Color BackgroundColor
        {
            get { return (Color)GetValue(BackgroundColorProperty); }
            set { SetValue(BackgroundColorProperty, value); }
        }
    }
}

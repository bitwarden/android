using System;
using Xamarin.Forms;

namespace Bit.App.Controls.BoxedView
{
    public class BaseCell : Cell
    {
        public static BindableProperty TitleProperty = BindableProperty.Create(
            nameof(Title), typeof(string), typeof(BaseCell), default(string), defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty TitleColorProperty = BindableProperty.Create(
            nameof(TitleColor), typeof(Color), typeof(BaseCell), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty TitleFontSizeProperty = BindableProperty.Create(
            nameof(TitleFontSize), typeof(double), typeof(BaseCell), -1.0, defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty BackgroundColorProperty = BindableProperty.Create(
                nameof(BackgroundColor), typeof(Color), typeof(BaseCell), default(Color),
                defaultBindingMode: BindingMode.OneWay);

        public string Title
        {
            get => (string)GetValue(TitleProperty);
            set => SetValue(TitleProperty, value);
        }

        public Color TitleColor
        {
            get => (Color)GetValue(TitleColorProperty);
            set => SetValue(TitleColorProperty, value);
        }

        [TypeConverter(typeof(FontSizeConverter))]
        public double TitleFontSize
        {
            get => (double)GetValue(TitleFontSizeProperty);
            set => SetValue(TitleFontSizeProperty, value);
        }

        public Color BackgroundColor
        {
            get => (Color)GetValue(BackgroundColorProperty);
            set => SetValue(BackgroundColorProperty, value);
        }

        public BoxedSection Section { get; set; }

        public new event EventHandler Tapped;

        protected internal new void OnTapped()
        {
            Tapped?.Invoke(this, EventArgs.Empty);
        }
    }
}

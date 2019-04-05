using System;
using System.Windows.Input;
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

        public static BindableProperty Button1IconProperty = BindableProperty.Create(
            nameof(Button1Icon), typeof(string), typeof(BaseCell), default(string),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button1CommandProperty = BindableProperty.Create(
            nameof(Button1Command), typeof(ICommand), typeof(BaseCell), default(ICommand),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button1CommandParameterProperty = BindableProperty.Create(
            nameof(Button1CommandParameter), typeof(object), typeof(BaseCell), default(object),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button2IconProperty = BindableProperty.Create(
            nameof(Button2Icon), typeof(string), typeof(BaseCell), default(string),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button2CommandProperty = BindableProperty.Create(
            nameof(Button2Command), typeof(ICommand), typeof(BaseCell), default(ICommand),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button2CommandParameterProperty = BindableProperty.Create(
            nameof(Button2CommandParameter), typeof(object), typeof(BaseCell), default(object),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button3IconProperty = BindableProperty.Create(
            nameof(Button3Icon), typeof(string), typeof(BaseCell), default(string),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button3CommandProperty = BindableProperty.Create(
            nameof(Button3Command), typeof(ICommand), typeof(BaseCell), default(ICommand),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty Button3CommandParameterProperty = BindableProperty.Create(
            nameof(Button3CommandParameter), typeof(object), typeof(BaseCell), default(object),
            defaultBindingMode: BindingMode.OneWay);

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

        public string Button1Icon
        {
            get => (string)GetValue(Button1IconProperty);
            set => SetValue(Button1IconProperty, value);
        }

        public ICommand Button1Command
        {
            get => (ICommand)GetValue(Button1CommandProperty);
            set => SetValue(Button1CommandProperty, value);
        }

        public object Button1CommandParameter
        {
            get => GetValue(Button1CommandParameterProperty);
            set => SetValue(Button1CommandParameterProperty, value);
        }

        public string Button2Icon
        {
            get => (string)GetValue(Button2IconProperty);
            set => SetValue(Button2IconProperty, value);
        }

        public ICommand Button2Command
        {
            get => (ICommand)GetValue(Button2CommandProperty);
            set => SetValue(Button2CommandProperty, value);
        }

        public object Button2CommandParameter
        {
            get => GetValue(Button2CommandParameterProperty);
            set => SetValue(Button2CommandParameterProperty, value);
        }

        public string Button3Icon
        {
            get => (string)GetValue(Button3IconProperty);
            set => SetValue(Button3IconProperty, value);
        }

        public ICommand Button3Command
        {
            get => (ICommand)GetValue(Button3CommandProperty);
            set => SetValue(Button3CommandProperty, value);
        }

        public object Button3CommandParameter
        {
            get => GetValue(Button3CommandParameterProperty);
            set => SetValue(Button3CommandParameterProperty, value);
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

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.Core.Models.Domain;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Controls
{
    public partial class IconLabelButton : Frame
    {
        public static readonly BindableProperty IconProperty = BindableProperty.Create(
            nameof(Icon), typeof(string), typeof(IconLabelButton));

        public static readonly BindableProperty LabelProperty = BindableProperty.Create(
            nameof(Label), typeof(string), typeof(IconLabelButton));

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(ICommand), typeof(IconLabelButton));

        public static readonly BindableProperty IconLabelColorProperty = BindableProperty.Create(
            nameof(IconLabelColor), typeof(Color), typeof(IconLabelButton), Color.White);

        public static readonly BindableProperty IconLabelBackgroundColorProperty = BindableProperty.Create(
            nameof(IconLabelBackgroundColor), typeof(Color), typeof(IconLabelButton), Color.White);

        public static readonly BindableProperty IconLabelBorderColorProperty = BindableProperty.Create(
            nameof(IconLabelBorderColor), typeof(Color), typeof(IconLabelButton), Color.White);

        public IconLabelButton()
        {
            InitializeComponent();
        }

        public string Icon
        {
            get => GetValue(IconProperty) as string;
            set => SetValue(IconProperty, value);
        }

        public string Label
        {
            get => GetValue(LabelProperty) as string;
            set => SetValue(LabelProperty, value);
        }

        public ICommand ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as ICommand;
            set => SetValue(ButtonCommandProperty, value);
        }

        public Color IconLabelColor
        {
            get { return (Color)GetValue(IconLabelColorProperty); }
            set { SetValue(IconLabelColorProperty, value); }
        }

        public Color IconLabelBackgroundColor
        {
            get { return (Color)GetValue(IconLabelBackgroundColorProperty); }
            set { SetValue(IconLabelBackgroundColorProperty, value); }
        }

        public Color IconLabelBorderColor
        {
            get { return (Color)GetValue(IconLabelBorderColorProperty); }
            set { SetValue(IconLabelBorderColorProperty, value); }
        }
    }
}


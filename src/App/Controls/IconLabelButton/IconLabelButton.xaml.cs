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
            nameof(ButtonCommand), typeof(Command), typeof(IconLabelButton));

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
    }
}


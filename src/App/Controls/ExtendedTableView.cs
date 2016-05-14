using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedTableView : TableView
    {
        public static readonly BindableProperty EnableScrollingProperty =
            BindableProperty.Create(nameof(EnableScrolling), typeof(bool), typeof(ExtendedTableView), true);

        public static readonly BindableProperty EnableSelectionProperty =
            BindableProperty.Create(nameof(EnableSelection), typeof(bool), typeof(ExtendedTableView), true);

        public static readonly BindableProperty SeparatorColorProperty =
            BindableProperty.Create(nameof(SeparatorColor), typeof(Color), typeof(ExtendedTableView), Color.FromHex("d2d6de"));

        public bool EnableScrolling
        {
            get { return (bool)GetValue(EnableScrollingProperty); }
            set { SetValue(EnableScrollingProperty, value); }
        }

        public bool EnableSelection
        {
            get { return (bool)GetValue(EnableSelectionProperty); }
            set { SetValue(EnableSelectionProperty, value); }
        }

        public Color SeparatorColor
        {
            get { return (Color)GetValue(SeparatorColorProperty); }
            set { SetValue(SeparatorColorProperty, value); }
        }

        public int EstimatedRowHeight { get; set; }
    }
}

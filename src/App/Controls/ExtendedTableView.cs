using System;
using Xamarin.Forms;
using System.Reflection;
using System.Reflection.Emit;
using System.Runtime.InteropServices;
using XLabs.Ioc;
using Bit.App.Abstractions;

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
        public bool NoHeader { get; set; }
        public bool NoFooter { get; set; }

        protected override SizeRequest OnSizeRequest(double widthConstraint, double heightConstraint)
        {
            if(!VerticalOptions.Expands && Device.RuntimePlatform != Device.UWP)
            {
                var reflectionService = Resolver.Resolve<IReflectionService>();
                var baseBaseOnSizeRequest = reflectionService.GetVisualElementOnSizeRequest(this);
                return baseBaseOnSizeRequest(widthConstraint, heightConstraint);
            }

            return base.OnSizeRequest(widthConstraint, heightConstraint);
        }
    }
}

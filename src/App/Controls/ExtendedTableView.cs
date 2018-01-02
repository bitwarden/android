using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;
using System;
using System.Reflection;

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

        public static readonly BindableProperty BottomPaddingProperty =
            BindableProperty.Create(nameof(BottomPadding), typeof(int), typeof(ExtendedTableView), 0);

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
        public int BottomPadding { get; set; }

        protected override SizeRequest OnSizeRequest(double widthConstraint, double heightConstraint)
        {
            if(!VerticalOptions.Expands && Device.RuntimePlatform != Device.UWP)
            {
                var baseOnSizeRequest = GetVisualElementOnSizeRequest();
                return baseOnSizeRequest(widthConstraint, heightConstraint);
            }

            return base.OnSizeRequest(widthConstraint, heightConstraint);
        }

        private Func<double, double, SizeRequest> GetVisualElementOnSizeRequest()
        {
            var handle = typeof(VisualElement).GetMethod(
                "OnSizeRequest",
                BindingFlags.Instance | BindingFlags.NonPublic,
                null,
                new Type[] { typeof(double), typeof(double) },
                null)?.MethodHandle;

            if(!handle.HasValue)
            {
                throw new ArgumentNullException("handle could not be found.");
            }

            var pointer = handle.Value.GetFunctionPointer();
            if(pointer == null)
            {
                throw new ArgumentNullException("pointer could not be found.");
            }

            return (Func<double, double, SizeRequest>)Activator.CreateInstance(
                typeof(Func<double, double, SizeRequest>), this, pointer);
        }
    }
}

using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Behaviors
{
    /// <summary>
    /// This behavior prevents the Editor to be automatically scrolled to the bottom on focus.
    /// This is needed due to this Xamarin Forms issue: https://github.com/xamarin/Xamarin.Forms/issues/2233
    /// </summary>
    public class EditorPreventAutoBottomScrollingOnFocusedBehavior : Behavior<Editor>
    {
        public static readonly BindableProperty ParentScrollViewProperty
            = BindableProperty.Create(nameof(ParentScrollView), typeof(ScrollView), typeof(EditorPreventAutoBottomScrollingOnFocusedBehavior));

        public ScrollView ParentScrollView
        {
            get => (ScrollView)GetValue(ParentScrollViewProperty);
            set => SetValue(ParentScrollViewProperty, value);
        }

        protected override void OnAttachedTo(Editor bindable)
        {
            base.OnAttachedTo(bindable);

            bindable.Focused += OnFocused;
        }

        private void OnFocused(object sender, FocusEventArgs e)
        {
            if (DeviceInfo.Platform.Equals(DevicePlatform.iOS) && ParentScrollView != null)
            {
                ParentScrollView.ScrollToAsync(ParentScrollView.ScrollX, ParentScrollView.ScrollY, true);
            }
        }

        protected override void OnDetachingFrom(Editor bindable)
        {
            bindable.Focused -= OnFocused;

            base.OnDetachingFrom(bindable);
        }
    }
}

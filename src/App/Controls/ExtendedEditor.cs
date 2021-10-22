using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedEditor : Editor
    {
        public static readonly BindableProperty IsScrollEnabledProperty = BindableProperty.Create(
               nameof(IsScrollEnabled), typeof(bool), typeof(ExtendedEditor), true);

        public bool IsScrollEnabled
        {
            get => (bool)GetValue(IsScrollEnabledProperty);
            set => SetValue(IsScrollEnabledProperty, value);
        }
    }
}

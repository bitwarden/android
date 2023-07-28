using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Utilities
{
    public static class ProgressBarExtensions
    {
        public static BindableProperty AnimatedProgressProperty =
           BindableProperty.CreateAttached("AnimatedProgress",
           typeof(double),
           typeof(ProgressBar),
           0.0d,
           BindingMode.OneWay,
           propertyChanged: (b, o, n) => ProgressBarProgressChanged((ProgressBar)b, (double)n));

        public static double GetAnimatedProgress(BindableObject target) => (double)target.GetValue(AnimatedProgressProperty);
        public static void SetAnimatedProgress(BindableObject target, double value) => target.SetValue(AnimatedProgressProperty, value);

        private static void ProgressBarProgressChanged(ProgressBar progressBar, double progress)
        {
            ViewExtensions.CancelAnimations(progressBar);
            progressBar.ProgressTo(progress, 500, Easing.SinIn);
        }
    }
}


using Microsoft.Maui.Graphics;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class ExtendedStepper : Stepper
    {
        public static readonly BindableProperty StepperBackgroundColorProperty = BindableProperty.Create(
            nameof(StepperBackgroundColor), typeof(Color), typeof(ExtendedStepper), Colors.White);

        public static readonly BindableProperty StepperForegroundColorProperty = BindableProperty.Create(
            nameof(StepperForegroundColor), typeof(Color), typeof(ExtendedStepper), Colors.Black);

        public Color StepperBackgroundColor
        {
            get => (Color)GetValue(StepperBackgroundColorProperty);
            set => SetValue(StepperBackgroundColorProperty, value);
        }

        public Color StepperForegroundColor
        {
            get => (Color)GetValue(StepperForegroundColorProperty);
            set => SetValue(StepperForegroundColorProperty, value);
        }
    }
}

using Bit.App.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class StepperCell : ExtendedViewCell
    {
        private Action _changedAction;

        public StepperCell(string labelText, double value, double min, double max, double increment, Action changed = null)
        {
            _changedAction = changed;

            Label = new Label
            {
                Text = labelText,
                HorizontalOptions = LayoutOptions.Start,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            };

            StepperValueLabel = new Label
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalTextAlignment = TextAlignment.Start,
                Text = value.ToString(),
                Style = (Style)Application.Current.Resources["text-muted"],
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            };

            Stepper = new Stepper
            {
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Minimum = min,
                Maximum = max,
                Increment = increment,
                Value = value
            };

            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal,
                Children = { Label, StepperValueLabel, Stepper },
                Spacing = 15,
                Padding = Helpers.OnPlatform(
                    iOS: new Thickness(15, 8),
                    Android: new Thickness(15, 2),
                    Windows: new Thickness(15, 8))
            };

            if(Device.RuntimePlatform == Device.Android)
            {
                Label.TextColor = Color.Black;
            }
            stackLayout.AdjustPaddingForDevice();

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public Label StepperValueLabel { get; private set; }
        public Stepper Stepper { get; private set; }

        private void Stepper_ValueChanged(object sender, ValueChangedEventArgs e)
        {
            StepperValueLabel.Text = e.NewValue.ToString();
            _changedAction?.Invoke();
        }

        public void InitEvents()
        {
            Stepper.ValueChanged += Stepper_ValueChanged;
        }

        public void Dispose()
        {
            Stepper.ValueChanged -= Stepper_ValueChanged;
        }
    }
}

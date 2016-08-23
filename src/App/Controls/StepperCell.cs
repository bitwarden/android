using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class StepperCell : ExtendedViewCell
    {
        public StepperCell(string labelText, double value, double min, double max, double increment)
        {
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

            Stepper.ValueChanged += Stepper_ValueChanged;

            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal,
                Children = { Label, StepperValueLabel, Stepper },
                Spacing = 15,
                Padding = Device.OnPlatform(
                    iOS: new Thickness(15, 8),
                    Android: new Thickness(15, 2),
                    WinPhone: new Thickness(15, 8))
            };

            if(Device.OS == TargetPlatform.Android)
            {
                Label.TextColor = Color.Black;
            }

            View = stackLayout;
        }

        private void Stepper_ValueChanged(object sender, ValueChangedEventArgs e)
        {
            StepperValueLabel.Text = e.NewValue.ToString();
        }

        public Label Label { get; private set; }
        public Label StepperValueLabel { get; private set; }
        public Stepper Stepper { get; private set; }
    }
}

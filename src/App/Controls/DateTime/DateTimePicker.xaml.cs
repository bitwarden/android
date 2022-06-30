using System.Runtime.CompilerServices;
using Xamarin.CommunityToolkit.UI.Views;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class DateTimePicker : Grid
    {
        public DateTimePicker()
        {
            InitializeComponent();
        }

        protected override void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (propertyName == nameof(BindingContext)
                &&
                BindingContext is DateTimeViewModel dateTimeViewModel)
            {
                AutomationProperties.SetName(_datePicker, dateTimeViewModel.DateName);
                AutomationProperties.SetName(_timePicker, dateTimeViewModel.TimeName);

                _datePicker.PlaceHolder = dateTimeViewModel.DatePlaceholder;
                _timePicker.PlaceHolder = dateTimeViewModel.TimePlaceholder;
            }
        }
    }

    public class LazyDateTimePicker : LazyView<DateTimePicker>
    {
    }
}

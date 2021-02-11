using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedTimePicker : TimePicker
    {
        private string _format;

        public static readonly BindableProperty PlaceHolderProperty = BindableProperty.Create(
            nameof(PlaceHolder), typeof(string), typeof(ExtendedTimePicker));

        public string PlaceHolder
        {
            get { return (string)GetValue(PlaceHolderProperty); }
            set { SetValue(PlaceHolderProperty, value); }
        }

        public static readonly BindableProperty NullableTimeProperty = BindableProperty.Create(
            nameof(NullableTime), typeof(TimeSpan?), typeof(ExtendedTimePicker));

        public TimeSpan? NullableTime
        {
            get { return (TimeSpan?)GetValue(NullableTimeProperty); }
            set
            {
                SetValue(NullableTimeProperty, value);
                UpdateTime();
            }
        }

        private void UpdateTime()
        {
            if (NullableTime.HasValue)
            {
                if (_format != null)
                {
                    Format = _format;
                }
            }
            else
            {
                Format = PlaceHolder;
            }
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();
            if (BindingContext != null)
            {
                _format = Format;
                UpdateTime();
            }
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (propertyName == TimeProperty.PropertyName || (propertyName == IsFocusedProperty.PropertyName &&
                                                              !IsFocused && (Time.ToString("t") ==
                                                                             DateTime.Now.TimeOfDay.ToString("t"))))
            {
                NullableTime = Time;
                UpdateTime();
            }

            if (propertyName == NullableTimeProperty.PropertyName)
            {
                if (NullableTime.HasValue)
                {
                    Time = NullableTime.Value;
                    if (Time.ToString(_format) == DateTime.Now.TimeOfDay.ToString(_format))
                    {
                        UpdateTime();
                    }
                }
                else
                {
                    UpdateTime();
                }
            }
        }
    }
}

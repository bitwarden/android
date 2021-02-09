using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedDatePicker : DatePicker
    {
        private string _format;

        public static readonly BindableProperty PlaceHolderProperty = BindableProperty.Create(
            nameof(PlaceHolder), typeof(string), typeof(ExtendedDatePicker));

        public string PlaceHolder
        {
            get { return (string)GetValue(PlaceHolderProperty); }
            set { SetValue(PlaceHolderProperty, value); }
        }

        public static readonly BindableProperty NullableDateProperty = BindableProperty.Create(
            nameof(NullableDate), typeof(DateTime?), typeof(ExtendedDatePicker));

        public DateTime? NullableDate
        {
            get { return (DateTime?)GetValue(NullableDateProperty); }
            set
            {
                SetValue(NullableDateProperty, value);
                UpdateDate();
            }
        }

        private void UpdateDate()
        {
            if (NullableDate.HasValue)
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
                UpdateDate();
            }
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (propertyName == DateProperty.PropertyName || (propertyName == IsFocusedProperty.PropertyName &&
                                                              !IsFocused && (Date.ToString("d") ==
                                                                             DateTime.Now.ToString("d"))))
            {
                NullableDate = Date;
                UpdateDate();
            }

            if (propertyName == NullableDateProperty.PropertyName)
            {
                if (NullableDate.HasValue)
                {
                    Date = NullableDate.Value;
                    if (Date.ToString(_format) == DateTime.Now.ToString(_format))
                    {
                        UpdateDate();
                    }
                }
                else
                {
                    UpdateDate();
                }
            }
        }
    }
}

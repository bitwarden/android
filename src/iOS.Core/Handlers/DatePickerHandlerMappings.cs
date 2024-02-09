using Bit.App.Controls;
using Microsoft.Maui.Handlers;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class DatePickerHandlerMappings
    {
        public static void Setup()
        {
            DatePickerHandler.Mapper.AppendToMapping("CustomDatePickerHandler", (handler, datePicker) =>
            {
                if (datePicker is ExtendedDatePicker extDatePicker)
                {
                    // center text
                    handler.PlatformView.TextAlignment = UITextAlignment.Center;

                    // use placeholder until NullableDate set 
                    if (!extDatePicker.NullableDate.HasValue)
                    {
                        handler.PlatformView.Text = extDatePicker.PlaceHolder;
                    }
                }
            });

            DatePickerHandler.Mapper.AppendToMapping(nameof(IDatePicker.Date), UpdateTextPlaceholderOnFormatLikePlacholder);

            DatePickerHandler.Mapper.AppendToMapping(nameof(IDatePicker.Format), UpdateTextPlaceholderOnFormatLikePlacholder);
        }

        private static void UpdateTextPlaceholderOnFormatLikePlacholder(IDatePickerHandler handler, IDatePicker datePicker)
        {
            if (datePicker is ExtendedDatePicker extDatePicker && extDatePicker.Format == extDatePicker.PlaceHolder)
            {
                handler.PlatformView.Text = extDatePicker.PlaceHolder;
            }
        }
    }
}

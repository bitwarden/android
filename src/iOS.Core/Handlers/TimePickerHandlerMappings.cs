using Bit.App.Controls;
using Microsoft.Maui.Handlers;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class TimePickerHandlerMappings
    {
        public static void Setup()
        {
            TimePickerHandler.Mapper.AppendToMapping("CustomDatePickerHandler", (handler, datePicker) =>
            {
                if (datePicker is ExtendedTimePicker extTimePicker)
                {
                    // center text
                    handler.PlatformView.TextAlignment = UITextAlignment.Center;

                    // use placeholder until NullableDate set 
                    if (!extTimePicker.NullableTime.HasValue)
                    {
                        handler.PlatformView.Text = extTimePicker.PlaceHolder;
                    }
                }
            });

            TimePickerHandler.Mapper.AppendToMapping(nameof(ITimePicker.Time), UpdateTextPlaceholderOnFormatLikePlacholder);

            TimePickerHandler.Mapper.AppendToMapping(nameof(ITimePicker.Format), UpdateTextPlaceholderOnFormatLikePlacholder);
        }

        private static void UpdateTextPlaceholderOnFormatLikePlacholder(ITimePickerHandler handler, ITimePicker timePicker)
        {
            if (timePicker is ExtendedTimePicker extDatePicker && extDatePicker.Format == extDatePicker.PlaceHolder)
            {
                handler.PlatformView.Text = extDatePicker.PlaceHolder;
            }
        }
    }
}


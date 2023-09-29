using Android.Views;
using Bit.App.Controls;
using Microsoft.Maui.Handlers;

namespace Bit.App.Handlers
{
    public class TimePickerHandlerMappings
    {
        public static void Setup()
        {
            TimePickerHandler.Mapper.AppendToMapping("CustomTimePickerHandler", (handler, timePicker) =>
            {
                if (timePicker is ExtendedTimePicker extTimePicker)
                {
                    // center text
                    handler.PlatformView.Gravity = GravityFlags.CenterHorizontal;

                    // use placeholder until NullableTime set 
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

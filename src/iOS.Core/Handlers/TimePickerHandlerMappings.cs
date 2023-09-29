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

                    // force use of wheel picker on iOS 14+
                    // TODO remove this when we upgrade to X.F 5 SR-1
                    // TODO: [Maui-Migration] Check if this is needed given that we're not on XForms anymore.
                    // https://github.com/xamarin/Xamarin.Forms/issues/12258#issuecomment-700168665
                    try
                    {
                        if (UIDevice.CurrentDevice.CheckSystemVersion(13, 2)
                            &&
                            handler.PlatformView.InputView is UIDatePicker picker)
                        {
                            picker.PreferredDatePickerStyle = UIDatePickerStyle.Wheels;
                        }
                    }
                    catch { }
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


using Android.App;
using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;

namespace Bit.App.Handlers
{
    // Note: This Handler exists only to allow the ExtendedDatePicker to receive IsFocused events on Android. iOS Already does this with this fix: https://github.com/dotnet/maui/pull/13321
    // If MAUI eventually implements this behavior we can remove this handler completely. There is another Handler (DatePickerHandlerMappings) for the other DatePicker customizations.
    public partial class ExtendedDatePickerHandler : DatePickerHandler
    {

        public static PropertyMapper<IDatePicker, ExtendedDatePickerHandler> PropertyMapper = new (DatePickerHandler.Mapper)
        {
            [nameof(IDatePicker.IsFocused)] = MapIsFocused
        };

        public ExtendedDatePickerHandler() : base(PropertyMapper)
        {
        }

        public static void MapIsFocused(ExtendedDatePickerHandler handler, IDatePicker datePicker)
        {
            if (handler.PlatformView.IsFocused == datePicker.IsFocused) return;
        
            if (datePicker.IsFocused)
            {
                handler.PlatformView.RequestFocus();
            }
            else
            {
                handler.PlatformView.ClearFocus();
            }
        }

        private DatePickerDialog? _dialog;

        protected override DatePickerDialog CreateDatePickerDialog(int year, int month, int day)
        {
            _dialog = base.CreateDatePickerDialog(year, month, day);
            return _dialog;
        }

        protected override void ConnectHandler(MauiDatePicker platformView)
        {
            base.ConnectHandler(platformView);
            if (_dialog != null)
            {
                _dialog.ShowEvent += OnDialogShown;
                _dialog.DismissEvent += OnDialogDismissed;
            }
        }

        //Currently the Disconnect Handler needs to be manually called from the App: https://github.com/dotnet/maui/issues/3604
        protected override void DisconnectHandler(MauiDatePicker platformView)
        {
            if (_dialog != null)
            {
                _dialog.ShowEvent -= OnDialogShown;
                _dialog.DismissEvent -= OnDialogDismissed;
            }
            base.DisconnectHandler(platformView);

            _dialog = null;
        }

        private void OnDialogShown(object sender, EventArgs e)
        {
            this.VirtualView.IsFocused = true;
        }

        private void OnDialogDismissed(object sender, EventArgs e)
        {
            this.VirtualView.IsFocused = false;
        }
    }
}

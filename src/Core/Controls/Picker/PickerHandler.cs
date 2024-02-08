#if ANDROID
using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;

namespace Bit.Core.Controls.Picker
{
    // HACK: Due to https://github.com/dotnet/maui/issues/19681 and not willing to use reflection to access
    // the alert dialog, we need to redefine the PickerHandler implementation for a custom one of ours
    // which handles showing the current selected item. Remove this workaround when MAUI releases a fix for this.
    // This is a copy from https://github.com/dotnet/maui/blob/main/src/Core/src/Handlers/Picker/PickerHandler.cs
    public partial class PickerHandler : ViewHandler<IPicker, MauiPicker>, IPickerHandler
    {
        public static IPropertyMapper<IPicker, IPickerHandler> Mapper = new PropertyMapper<IPicker, PickerHandler>(ViewMapper)
        {
#if __ANDROID__ || WINDOWS
            [nameof(IPicker.Background)] = MapBackground,
#endif
            [nameof(IPicker.CharacterSpacing)] = MapCharacterSpacing,
            [nameof(IPicker.Font)] = MapFont,
            [nameof(IPicker.SelectedIndex)] = MapSelectedIndex,
            [nameof(IPicker.TextColor)] = MapTextColor,
            [nameof(IPicker.Title)] = MapTitle,
            [nameof(IPicker.TitleColor)] = MapTitleColor,
            [nameof(ITextAlignment.HorizontalTextAlignment)] = MapHorizontalTextAlignment,
            [nameof(ITextAlignment.VerticalTextAlignment)] = MapVerticalTextAlignment,
            [nameof(IPicker.Items)] = MapItems,
        };

        public static CommandMapper<IPicker, IPickerHandler> CommandMapper = new(ViewCommandMapper)
        {
        };

        public PickerHandler() : base(Mapper, CommandMapper)
        {
        }

        public PickerHandler(IPropertyMapper? mapper)
            : base(mapper ?? Mapper, CommandMapper)
        {
        }

        public PickerHandler(IPropertyMapper? mapper, CommandMapper? commandMapper)
            : base(mapper ?? Mapper, commandMapper ?? CommandMapper)
        {
        }

        IPicker IPickerHandler.VirtualView => VirtualView;

        Microsoft.Maui.Platform.MauiPicker IPickerHandler.PlatformView => PlatformView;
    }
}

#endif
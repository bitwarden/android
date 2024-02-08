using Bit.App.Controls;
using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public partial class SelectableLabelHandler : ViewHandler<SelectableLabel, UITextView>
    {
        public static PropertyMapper<SelectableLabel, SelectableLabelHandler> PropertyMapper = new PropertyMapper<SelectableLabel, SelectableLabelHandler>(ViewHandler.ViewMapper)
        {
            [nameof(SelectableLabel.Text)] = MapText,
            [nameof(SelectableLabel.TextColor)] = MapTextColor,
            [nameof(SelectableLabel.FontAttributes)] = MapFontAttributes,
        };

        public SelectableLabelHandler() : base(PropertyMapper)
        {
        }

        protected override UITextView CreatePlatformView()
        {
            var uiTextView = new UITextView();
            uiTextView.Selectable = true;
            uiTextView.Editable = false;
            uiTextView.ScrollEnabled = false;
            uiTextView.TextContainerInset = UIEdgeInsets.Zero;
            uiTextView.TextContainer.LineFragmentPadding = 0;
            uiTextView.BackgroundColor = UIColor.Clear;
            return uiTextView;
        }

        private static void MapText(SelectableLabelHandler handler, SelectableLabel label)
        {
            handler.PlatformView.Text = label.Text;
        }

        private static void MapTextColor(SelectableLabelHandler handler, SelectableLabel label)
        {
            handler.PlatformView.TextColor = label.TextColor.ToPlatform();
        }

        private static void MapFontAttributes(SelectableLabelHandler handler, SelectableLabel label)
        {
            switch (label.FontAttributes)
            {
                case FontAttributes.None:
                    handler.PlatformView.Font = UIFont.SystemFontOfSize(new nfloat(label.FontSize));
                    break;
                case FontAttributes.Bold:
                    handler.PlatformView.Font = UIFont.BoldSystemFontOfSize(new nfloat(label.FontSize));
                    break;
                case FontAttributes.Italic:
                    handler.PlatformView.Font = UIFont.ItalicSystemFontOfSize(new nfloat(label.FontSize));
                    break;
                default:
                    handler.PlatformView.Font = UIFont.BoldSystemFontOfSize(new nfloat(label.FontSize));
                    break;
            }
        }
    }
}

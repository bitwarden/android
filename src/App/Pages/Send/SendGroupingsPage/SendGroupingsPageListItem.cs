using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Pages
{
    public class SendGroupingsPageListItem : ISendGroupingsPageListItem
    {
        private string _icon;
        private string _name;

        public SendView Send { get; set; }
        public SendType? Type { get; set; }
        public string ItemCount { get; set; }
        public bool ShowOptions { get; set; }

        public string Name
        {
            get
            {
                if (_name != null)
                {
                    return _name;
                }
                if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case SendType.Text:
                            _name = AppResources.TypeText;
                            break;
                        case SendType.File:
                            _name = AppResources.TypeFile;
                            break;
                        default:
                            break;
                    }
                }
                return _name;
            }
        }

        public string Icon
        {
            get
            {
                if (_icon != null)
                {
                    return _icon;
                }
                if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case SendType.Text:
                            _icon = BitwardenIcons.FileText;
                            break;
                        case SendType.File:
                            _icon = BitwardenIcons.File;
                            break;
                        default:
                            break;
                    }
                }
                return _icon;
            }
        }

        public string AutomationId
        {
            get
            {
                if (_name != null)
                {
                    return "SendItem";
                }
                if (Type != null)
                {
                    switch (Type.Value)
                    {
                        case SendType.Text:
                            return "SendTextFilter";
                        case SendType.File:
                            return "SendFileFilter";
                    }
                }
                return null;
            }
        }
    }
}

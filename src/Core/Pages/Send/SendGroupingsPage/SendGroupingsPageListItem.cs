using Bit.App.Controls;
using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;

namespace Bit.App.Pages
{
    public class SendGroupingsPageListItem : ISendGroupingsPageListItem
    {
        private string _icon;
        private string _name;

        public SendGroupingsPageListItem(SendType type, int itemCount)
        {
            Type = type;
            ItemCount = itemCount.ToString("N0");
        }

        public SendGroupingsPageListItem(SendView send, bool showOptions)
        {
            SendItemViewModel = new SendViewCellViewModel(send, showOptions);
        }

        //public SendView Send { get; set; }
        public SendType? Type { get; set; }
        public SendViewCellViewModel SendItemViewModel { get; }
        public string ItemCount { get; set; }
        //public bool ShowOptions { get; set; }

        public string Name
        {
            get
            {
                if (_name != null)
                {
                    return _name;
                }

                if (!Type.HasValue)
                {
                    return null;
                }

                switch (Type.Value)
                {
                    case SendType.Text:
                        _name = AppResources.TypeText;
                        break;
                    case SendType.File:
                        _name = AppResources.TypeFile;
                        break;
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

                if (!Type.HasValue)
                {
                    return null;
                }

                switch (Type.Value)
                {
                    case SendType.Text:
                        _icon = BitwardenIcons.FileText;
                        break;
                    case SendType.File:
                        _icon = BitwardenIcons.File;
                        break;
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

using Bit.App.Resources;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Pages
{
    public class SendGroupingsPageListItem
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
                            _icon = "\uf0f6"; // fa-file-text-o
                            break;
                        case SendType.File:
                            _icon = "\uf016"; // fa-file-o
                            break;
                        default:
                            break;
                    }
                }
                return _icon;
            }
        }
    }
}

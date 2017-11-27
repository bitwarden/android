using Bit.App.Resources;
using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class AddCipherToolBarItem : ExtendedToolbarItem
    {
        public AddCipherToolBarItem(Page page, string folderId)
            : base(() => Helpers.AddCipher(page, folderId))
        {
            Text = AppResources.Add;
            Icon = "plus.png";
        }
    }
}

using Bit.App.Resources;
using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class AddCipherToolbarItem : ExtendedToolbarItem
    {
        public AddCipherToolbarItem(Page page, string folderId)
            : base(() => Helpers.AddCipher(page, folderId))
        {
            Text = AppResources.Add;
            Icon = "plus.png";
        }
    }
}

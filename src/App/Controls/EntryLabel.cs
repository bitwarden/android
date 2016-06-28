using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class EntryLabel : Label
    {
        public EntryLabel()
        {
            FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label));
            TextColor = Color.FromHex("777777");
        }
    }
}

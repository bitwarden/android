using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App
{
    public partial class ExtensionApp : Application
    {
        public ExtensionApp()
        {
            InitializeComponent();
            ThemeManager.SetTheme(false, Current.Resources);
        }
    }
}

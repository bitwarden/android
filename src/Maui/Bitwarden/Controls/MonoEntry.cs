using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class MonoEntry : Entry
    {
        public MonoEntry()
        {
#if ANDROID
            FontFamily = "RobotoMono_Regular";
#elif IOS
            FontFamily = "Menlo-Regular";
#endif
        }
    }
}

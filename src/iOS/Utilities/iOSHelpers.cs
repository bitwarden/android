using UIKit;
using Xamarin.Forms;

namespace Bit.iOS.Utilities
{
    public static class iOSHelpers
    {
        public static System.nfloat? GetAccessibleFont<T>(double size)
        {
            var pointSize = UIFontDescriptor.PreferredBody.PointSize;
            if(size == Device.GetNamedSize(NamedSize.Large, typeof(T)))
            {
                pointSize *= 1.3f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Small, typeof(T)))
            {
                pointSize *= .8f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Micro, typeof(T)))
            {
                pointSize *= .6f;
            }
            else if(size != Device.GetNamedSize(NamedSize.Default, typeof(T)))
            {
                // not using dynamic font sizes, return
                return null;
            }
            return pointSize;
        }
    }
}
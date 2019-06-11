using UIKit;

namespace Bit.iOS
{
    public class Application
    {
        static void Main(string[] args)
        {
            ObjCRuntime.Dlfcn.dlopen(ObjCRuntime.Constants.libSystemLibrary, 0);
            UIApplication.Main(args, null, "AppDelegate");
        }
    }
}

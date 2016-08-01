using System;
using System.Collections.Generic;
using System.Linq;

using Foundation;
using UIKit;

namespace Bit.iOS
{
    public class Application
    {
        // This is the main entry point of the application.
        static void Main(string[] args)
        {
            ObjCRuntime.Dlfcn.dlopen(ObjCRuntime.Constants.libSystemLibrary, 0);

            // if you want to use a different Application Delegate class from "AppDelegate"
            // you can specify it here.
            UIApplication.Main(args, null, "AppDelegate");
        }
    }
}

using System;
using System.IO;
using System.Reflection;
using Bit.UITests.Helpers;
using Bit.UITests.Setup.SimulatorManager;
using Xamarin.UITest;

namespace Bit.UITests.Setup
{
    internal static class AppManager
    {
        private static readonly string _slnPath = GetSlnPath();

        private const string AndroidPackageName = "com.x8bit.bitwarden";
        private const string IosBundleId = "com.x8bit.bitwarden";
        private static readonly string _apkPath = Path.Combine(_slnPath, "Android", "bin", "release", $"{AndroidPackageName}-Signed.apk");
        private static readonly string _iosPath = Path.Combine("..", "..", "..", $"{IosBundleId}.app");

        private static IApp _app;

        private static Platform? _platform;

        public static IApp App
        {
            get
            {
                if (_app == null)
                {
                    throw new NullReferenceException("'AppManager.App' not set. Call 'AppManager.StartApp()' before trying to access it.");
                }

                return _app;
            }
        }


        public static Platform Platform
        {
            get
            {
                if (_platform == null)
                {
                    throw new NullReferenceException("'AppManager.Platform' not set.");
                }

                return _platform.Value;
            }

            set => _platform = value;
        }

        public static void StartApp()
        {
            Console.WriteLine($"TestEnvironment.IsTestCloud: {TestEnvironment.IsTestCloud}");
            Console.WriteLine($"TestEnvironment.Platform: {TestEnvironment.Platform}");
            Console.WriteLine($"Platform: {Platform}");

            switch (Platform, TestEnvironment.IsTestCloud)
            {
                case (Platform.Android, false):
                    _app = ConfigureApp
                        .Android
                        .InstalledApp(AndroidPackageName)
                        //.ApkFile(_apkPath)
                        .StartApp();
                    break;
                case (Platform.iOS, false):
                    _app = ConfigureApp
                        .iOS
                        .SetDeviceByName("iPhone X") //NOTE Get Devices name in terminal: xcrun instruments -s devices
                        .AppBundle(_iosPath)
                        // .InstalledApp(IosBundleId)
                        .StartApp();
                    break;
                case (Platform.Android, true):
                    _app = ConfigureApp.Android.WaitTimes(new CustomWaitTimes()).StartApp();
                    break;
                case (Platform.iOS, true):
                    _app = ConfigureApp.iOS.WaitTimes(new CustomWaitTimes()).StartApp();
                    break;
            }
        }


        private static string GetSlnPath()
        {
            string currentFile = new Uri(Assembly.GetExecutingAssembly().CodeBase).LocalPath;
            var fi = new FileInfo(currentFile);
            string path = fi.Directory!.Parent!.Parent!.Parent!.FullName;
            return path;
        }
    }
}

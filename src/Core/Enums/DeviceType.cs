using System.Collections.Generic;
using System.Linq;

namespace Bit.Core.Enums
{
    public enum DeviceType : byte
    {
        Android = 0,
        iOS = 1,
        ChromeExtension = 2,
        FirefoxExtension = 3,
        OperaExtension = 4,
        EdgeExtension = 5,
        WindowsDesktop = 6,
        MacOsDesktop = 7,
        LinuxDesktop = 8,
        ChromeBrowser = 9,
        FirefoxBrowser = 10,
        OperaBrowser = 11,
        EdgeBrowser = 12,
        IEBrowser = 13,
        UnknownBrowser = 14,
        AndroidAmazon = 15,
        UWP = 16,
        SafariBrowser = 17,
        VivaldiBrowser = 18,
        VivaldiExtension = 19,
        SafariExtension = 20
    }

    public static class DeviceTypeExtensions
    {
        public static List<DeviceType> GetMobileTypes() => new List<DeviceType>
            {
                DeviceType.Android,
                DeviceType.AndroidAmazon,
                DeviceType.iOS
            };

        public static List<DeviceType> GetDesktopTypes() => new List<DeviceType>
            {
                DeviceType.WindowsDesktop,
                DeviceType.MacOsDesktop,
                DeviceType.LinuxDesktop,
                DeviceType.UWP,
            };

        public static List<DeviceType> GetDesktopAndMobileTypes() => GetMobileTypes().Concat(GetDesktopTypes()).ToList();
    }
}

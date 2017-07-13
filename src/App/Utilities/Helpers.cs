using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class Helpers
    {
        public static readonly DateTime Epoc = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long EpocUtcNow()
        {
            return (long)(DateTime.UtcNow - Epoc).TotalMilliseconds;
        }

        public static T OnPlatform<T>(T iOS = default(T), T Android = default(T),
            T WinPhone = default(T), T Windows = default(T))
        {
            switch(Device.RuntimePlatform)
            {
                case Device.iOS:
                    return iOS;
                case Device.Android:
                    return Android;
                case Device.WinPhone:
                    return WinPhone;
                case Device.Windows:
                    return Windows;
                default:
                    throw new Exception("Unsupported platform.");
            }
        }
    }
}

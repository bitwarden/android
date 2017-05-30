using System;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class Helpers
    {
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

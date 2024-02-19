using System;
using System.Runtime.InteropServices;
using Bit.App.Utilities;
using Bit.Core.Services;
using Microsoft.Maui.Controls.Compatibility.Platform.iOS;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class iOSHelpers
    {
        [DllImport(ObjCRuntime.Constants.SystemLibrary)]
        internal static extern int sysctlbyname([MarshalAs(UnmanagedType.LPStr)] string property, IntPtr output,
            IntPtr oldLen, IntPtr newp, uint newLen);

        // Returns the difference between when the system was booted and now in seconds, resulting in a duration that
        // includes sleep time.
        // ref: https://forums.xamarin.com/discussion/20006/access-to-sysctl-h
        // ref: https://github.com/XLabs/Xamarin-Forms-Labs/blob/master/src/Platform/XLabs.Platform.iOS/Device/AppleDevice.cs
        public static long? GetSystemUpTimeMilliseconds()
        {
            long? uptime = null;
            IntPtr pLen = default, pStr = default;
            try
            {
                var property = "kern.boottime";
                pLen = Marshal.AllocHGlobal(sizeof(int));
                sysctlbyname(property, IntPtr.Zero, pLen, IntPtr.Zero, 0);
                var length = Marshal.ReadInt32(pLen);
                pStr = Marshal.AllocHGlobal(length);
                sysctlbyname(property, pStr, pLen, IntPtr.Zero, 0);
                var timeVal = Marshal.PtrToStructure<TimeVal>(pStr);
                var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                if (timeVal.sec > 0 && now > 0)
                {
                    uptime = (now - timeVal.sec) * 1000;
                }
            }
            catch (Exception e)
            {
                Logger.Instance.Exception(e);
            }
            finally
            {
                if (pLen != default)
                {
                    Marshal.FreeHGlobal(pLen);
                }
                if (pStr != default)
                {
                    Marshal.FreeHGlobal(pStr);
                }
            }
            return uptime;
        }

        public static nfloat? GetAccessibleFont<T>(double size)
        {
            var pointSize = UIFontDescriptor.PreferredBody.PointSize;
            if (size == Device.GetNamedSize(NamedSize.Large, typeof(T)))
            {
                pointSize *= 1.3f;
            }
            else if (size == Device.GetNamedSize(NamedSize.Small, typeof(T)))
            {
                pointSize *= .8f;
            }
            else if (size == Device.GetNamedSize(NamedSize.Micro, typeof(T)))
            {
                pointSize *= .6f;
            }
            else if (size != Device.GetNamedSize(NamedSize.Default, typeof(T)))
            {
                // not using dynamic font sizes, return
                return null;
            }
            return pointSize;
        }

        public static void SetBottomBorder(UITextField control)
        {
            control.BorderStyle = UITextBorderStyle.None;
            SetBottomBorder(control as UIView);
        }

        public static void SetBottomBorder(UITextView control)
        {
            SetBottomBorder(control as UIView);
        }

        private static void SetBottomBorder(UIView control)
        {
            var borderLine = new UIView
            {
                BackgroundColor = ThemeManager.GetResourceColor("BoxBorderColor").ToUIColor(),
                TranslatesAutoresizingMaskIntoConstraints = false
            };
            control.AddSubview(borderLine);
            control.AddConstraints(new NSLayoutConstraint[]
            {
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Height, NSLayoutRelation.Equal, 1, 1f),
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Leading, NSLayoutRelation.Equal,
                    control, NSLayoutAttribute.Leading, 1, 0),
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal,
                    control, NSLayoutAttribute.Trailing, 1, 0),
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Top, NSLayoutRelation.Equal,
                    control, NSLayoutAttribute.Bottom, 1, 10f),
            });
        }
        
        private struct TimeVal
        {
            public long sec;
            public long usec;
        }
    }
}

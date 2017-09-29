using Bit.App.Abstractions;
using Microsoft.Toolkit.Uwp.Helpers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.Graphics.Display;
using Windows.Devices.SmartCards;
using Windows.Devices.Enumeration;

namespace Bit.UWP.Services
{
    public class DeviceInfoService : IDeviceInfoService
    {
        public string Model => SystemInformation.DeviceModel;

        public int Version => SystemInformation.OperatingSystemVersion.Build;

        public float Scale => (float)DisplayInformation.GetForCurrentView().RawPixelsPerViewPixel;

        public bool NfcEnabled
        {
            get
            {
                if (!Windows.Foundation.Metadata.ApiInformation.IsTypePresent("Windows.Devices.SmartCards.SmartCardEmulator"))
                    return false;

                return SmartCardEmulator.GetDefaultAsync().GetResults() != null;
            }
        }

        public bool HasCamera => DeviceInformation.FindAllAsync(DeviceClass.VideoCapture).GetResults().Any();
    }
}

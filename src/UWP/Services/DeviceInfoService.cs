using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.UWP.Services
{
    public class DeviceInfoService : IDeviceInfoService
    {
        public string Model => "6S";

        public int Version => 0;

        public float Scale => 1;

        public bool NfcEnabled => false;

        public bool HasCamera => true;
    }
}

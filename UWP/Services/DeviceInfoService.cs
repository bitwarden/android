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
        public string Model => throw new NotImplementedException();

        public int Version => throw new NotImplementedException();

        public float Scale => throw new NotImplementedException();

        public bool NfcEnabled => throw new NotImplementedException();

        public bool HasCamera => throw new NotImplementedException();
    }
}

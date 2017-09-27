using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.UWP.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        public bool CanOpenFile(string fileName)
        {
            throw new NotImplementedException();
        }

        public void ClearCache()
        {
            throw new NotImplementedException();
        }

        public void CopyToClipboard(string text)
        {
            throw new NotImplementedException();
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            throw new NotImplementedException();
        }

        public Task SelectFileAsync()
        {
            throw new NotImplementedException();
        }
    }
}

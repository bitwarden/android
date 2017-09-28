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
            return false;
        }

        public void ClearCache()
        {
        }

        public void CopyToClipboard(string text)
        {
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            return false;
        }

        public Task SelectFileAsync()
        {
            return Task.CompletedTask;
        }
    }
}

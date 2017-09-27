using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.UWP.Services
{
    public class SecureStorageService : ISecureStorageService
    {
        public bool Contains(string key)
        {
            throw new NotImplementedException();
        }

        public void Delete(string key)
        {
            throw new NotImplementedException();
        }

        public byte[] Retrieve(string key)
        {
            throw new NotImplementedException();
        }

        public void Store(string key, byte[] dataBytes)
        {
            throw new NotImplementedException();
        }
    }
}

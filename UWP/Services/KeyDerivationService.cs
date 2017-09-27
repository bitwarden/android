using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.UWP.Services
{
    public class KeyDerivationService : IKeyDerivationService
    {
        public byte[] DeriveKey(byte[] password, byte[] salt, uint rounds)
        {
            throw new NotImplementedException();
        }
    }
}

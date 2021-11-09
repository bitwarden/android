using System;
namespace Bit.Core.Models.Request
{
    public class KeyConnectorUserKeyRequest
    {
        public string Key { get; set; }

        public KeyConnectorUserKeyRequest(string key)
        {
            Key = key;
        }
    }
}

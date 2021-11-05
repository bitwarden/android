using System;
using Bit.Core.Enums;

namespace Bit.Core.Models.Request
{
    public class SetKeyConnectorKeyRequest
    {
        public string Key { get; set; }
        public KeysRequest Keys { get; set; }
        public KdfType Kdf { get; set; }
        public int? KdfIterations { get; set; }
        public string OrgIdentifier { get; set; }

        public SetKeyConnectorKeyRequest(string key, KeysRequest keys,
            KdfType kdf, int? kdfIterations, string orgIdentifier)
        {
            this.Key = key;
            this.Keys = keys;
            this.Kdf = kdf;
            this.KdfIterations = kdfIterations;
            this.OrgIdentifier = orgIdentifier;
        }
    }
}

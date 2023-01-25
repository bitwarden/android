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
        public int? KdfMemory { get; set; }
        public int? KdfParallelism { get; set; }
        public string OrgIdentifier { get; set; }

        public SetKeyConnectorKeyRequest(string key, KeysRequest keys,
            KdfType kdf, int? kdfIterations, int? kdfMemory, int? kdfParallelism, string orgIdentifier)
        {
            this.Key = key;
            this.Keys = keys;
            this.Kdf = kdf;
            this.KdfIterations = kdfIterations;
            this.KdfMemory = kdfMemory;
            this.KdfParallelism = kdfParallelism;
            this.OrgIdentifier = orgIdentifier;
        }
    }
}

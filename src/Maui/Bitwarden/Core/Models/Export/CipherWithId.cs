using System.Collections.Generic;
using Bit.Core.Models.View;
using Newtonsoft.Json;

namespace Bit.Core.Models.Export
{
    public class CipherWithId : Cipher
    {
        public CipherWithId(CipherView obj) : base(obj)
        {
            Id = obj.Id;
            CollectionIds = null;
        }

        public CipherWithId(Domain.Cipher obj) : base(obj)
        {
            Id = obj.Id;
            CollectionIds = null;
        }

        [JsonProperty(Order = int.MinValue)]
        public string Id { get; set; }
        [JsonProperty(Order = int.MaxValue)]
        public HashSet<string> CollectionIds { get; set; }
    }
}

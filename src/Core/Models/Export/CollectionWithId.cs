﻿using Bit.Core.Models.View;
using Newtonsoft.Json;

namespace Bit.Core.Models.Export
{
    public class CollectionWithId : Collection
    {
        [JsonProperty(Order = int.MinValue)]
        public string Id { get; set; }
        
        public CollectionWithId(CollectionView obj) : base(obj)
        {
            Id = obj.Id;
        }
    }
}

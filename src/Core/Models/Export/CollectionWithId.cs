using Bit.Core.Models.View;
using Newtonsoft.Json;
using CollectionView = Bit.Core.Models.View.CollectionView;

namespace Bit.Core.Models.Export
{
    public class CollectionWithId : Collection
    {
        public CollectionWithId(CollectionView obj) : base(obj)
        {
            Id = obj.Id;
        }

        public CollectionWithId(Domain.Collection obj) : base(obj)
        {
            Id = obj.Id;
        }

        [JsonProperty(Order = int.MinValue)]
        public string Id { get; set; }
    }
}

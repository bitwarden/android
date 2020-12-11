using Bit.Core.Models.View;
using Newtonsoft.Json;

namespace Bit.Core.Models.Export
{
    public class FolderWithId : Folder
    {
        public FolderWithId(FolderView obj) : base(obj)
        {
            Id = obj.Id;
        }

        public FolderWithId(Domain.Folder obj) : base(obj)
        {
            Id = obj.Id;
        }

        [JsonProperty(Order = int.MinValue)]
        public string Id { get; set; }
    }
}

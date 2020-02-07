using Bit.Core.Models.View;
using Newtonsoft.Json;

namespace Bit.Core.Models.Export
{
    public class FolderWithId : Folder
    {
        [JsonProperty(Order = int.MinValue)]
        public string Id { get; set; }

        public FolderWithId(FolderView obj) : base(obj)
        {
            Id = obj.Id;
        }
    }
}

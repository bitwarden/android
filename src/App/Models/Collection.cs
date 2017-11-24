using Bit.App.Models.Data;
using Bit.App.Models.Api;

namespace Bit.App.Models
{
    public class Collection
    {
        public Collection()
        { }

        public Collection(CollectionData data)
        {
            Id = data.Id;
            OrganizationId = data.OrganizationId;
            Name = data.Name != null ? new CipherString(data.Name) : null;
        }

        public Collection(CollectionResponse response)
        {
            Id = response.Id;
            OrganizationId = response.OrganizationId;
            Name = response.Name != null ? new CipherString(response.Name) : null;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public CipherString Name { get; set; }
    }
}

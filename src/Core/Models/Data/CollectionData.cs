using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class CollectionData : Data
    {
        public CollectionData() { }

        public CollectionData(CollectionDetailsResponse response)
        {
            Id = response.Id;
            OrganizationId = response.OrganizationId;
            Name = response.Name;
            ExternalId = response.ExternalId;
            ReadOnly = response.ReadOnly;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string ExternalId { get; set; }
        public bool ReadOnly { get; set; }
    }
}

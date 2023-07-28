namespace Bit.Core.Models.Response
{
    public class CollectionResponse
    {
        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string ExternalId { get; set; }
    }

    public class CollectionDetailsResponse : CollectionResponse
    {
        public bool ReadOnly { get; set; }
    }
}

using SQLite;
using Bit.App.Abstractions;
using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    [Table("Collection")]
    public class CollectionData : IDataObject<string>
    {
        public CollectionData()
        { }

        public CollectionData(Collection collection, string userId)
        {
            Id = collection.Id;
            UserId = userId;
            Name = collection.Name?.EncryptedString;
            OrganizationId = collection.OrganizationId;
        }

        public CollectionData(CollectionResponse collection, string userId)
        {
            Id = collection.Id;
            UserId = userId;
            Name = collection.Name;
            OrganizationId = collection.OrganizationId;
        }

        [PrimaryKey]
        public string Id { get; set; }
        [Indexed]
        public string UserId { get; set; }
        public string Name { get; set; }
        public string OrganizationId { get; set; }
    }
}

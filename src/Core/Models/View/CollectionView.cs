using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class CollectionView : View, ITreeNodeObject
    {
        public CollectionView() { }

        public CollectionView(Collection c)
        {
            Id = c.Id;
            OrganizationId = c.OrganizationId;
            ReadOnly = c.ReadOnly;
            ExternalId = c.ExternalId;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string ExternalId { get; set; }
        public bool ReadOnly { get; set; }
    }
}

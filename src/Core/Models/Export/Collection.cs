using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Collection
    {
        public Collection() { }

        public Collection(CollectionView obj)
        {
            OrganizationId = obj.OrganizationId;
            Name = obj.Name;
            ExternalId = obj.ExternalId;
        }

        public Collection(Domain.Collection obj)
        {
            OrganizationId = obj.OrganizationId;
            Name = obj.Name?.EncryptedString;
            ExternalId = obj.ExternalId;
        }

        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string ExternalId { get; set; }

        public CollectionView ToView(Collection req, CollectionView view = null)
        {
            if (view == null)
            {
                view = new CollectionView();
            }

            view.Name = req.Name;
            view.ExternalId = req.ExternalId;
            if (view.OrganizationId == null)
            {
                view.OrganizationId = req.OrganizationId;
            }

            return view;
        }
    }
}

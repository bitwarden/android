using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Collection
    {
        public Collection()
        {
            OrganizationId = "00000000-0000-0000-0000-000000000000";
            Name = "Collection name";
            ExternalId = null;
        }

        public CollectionView ToView(Collection req, CollectionView view = null)
        {
            if(view == null)
            {
                view = new CollectionView();
            }

            view.Name = req.Name;
            view.ExternalId = req.ExternalId;
            if(view.OrganizationId == null)
            {
                view.OrganizationId = req.OrganizationId;
            }

            return view;
        }

        public string OrganizationId { get; set; }
        public string Name { get; set; }
        public string ExternalId { get; set; }

        public Collection(CollectionView obj)
        {
            OrganizationId = obj.OrganizationId;
            Name = obj.Name;
            ExternalId = obj.ExternalId;
        }
    }
}

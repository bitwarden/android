using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class Collection : Domain
    {
        public Collection() { }

        public Collection(CollectionData obj, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, obj, new HashSet<string>
            {
                "Id",
                "OrganizationId",
                "Name",
                "ExternalId",
                "ReadOnly"
            }, alreadyEncrypted, new HashSet<string>
            {
                "Id",
                "OrganizationId",
                "ExternalId",
                "ReadOnly"
            });
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public EncString Name { get; set; }
        public string ExternalId { get; set; }
        public bool ReadOnly { get; set; }

        public Task<CollectionView> DecryptAsync()
        {
            return DecryptObjAsync(new CollectionView(this), this, new HashSet<string> { "Name" }, OrganizationId);
        }
    }
}

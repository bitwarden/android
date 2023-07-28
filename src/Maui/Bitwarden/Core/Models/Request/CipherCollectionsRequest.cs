using System.Collections.Generic;

namespace Bit.Core.Models.Request
{
    public class CipherCollectionsRequest
    {
        public CipherCollectionsRequest(List<string> collectionIds)
        {
            CollectionIds = collectionIds ?? new List<string>();
        }

        public List<string> CollectionIds { get; set; }
    }
}

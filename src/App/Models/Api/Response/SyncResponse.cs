using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class SyncResponse
    {
        public ProfileResponse Profile { get; set; }
        public IEnumerable<FolderResponse> Folders { get; set; }
        public IEnumerable<CollectionResponse> Collections { get; set; }
        public IEnumerable<CipherResponse> Ciphers { get; set; }
        public DomainsResponse Domains { get; set; }
    }
}

using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class SyncResponse
    {
        public ProfileResponse Profile { get; set; }
        public List<FolderResponse> Folders { get; set; } = new List<FolderResponse>();
        public List<CollectionResponse> Collections { get; set; } = new List<CollectionResponse>();
        public List<CipherResponse> Ciphers { get; set; } = new List<CipherResponse>();
        public DomainsResponse Domains { get; set; }
    }
}

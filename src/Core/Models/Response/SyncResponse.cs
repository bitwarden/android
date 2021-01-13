using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class SyncResponse
    {
        public ProfileResponse Profile { get; set; }
        public List<FolderResponse> Folders { get; set; } = new List<FolderResponse>();
        public List<CollectionDetailsResponse> Collections { get; set; } = new List<CollectionDetailsResponse>();
        public List<CipherResponse> Ciphers { get; set; } = new List<CipherResponse>();
        public DomainsResponse Domains { get; set; }
        public List<PolicyResponse> Policies { get; set; } = new List<PolicyResponse>();
        public List<SendResponse> Sends { get; set; } = new List<SendResponse>();
    }
}

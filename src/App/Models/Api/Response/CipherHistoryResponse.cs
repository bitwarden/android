using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class CipherHistoryResponse
    {
        public IEnumerable<CipherResponse> Revised { get; set; }
        public IEnumerable<string> Deleted { get; set; }
    }
}

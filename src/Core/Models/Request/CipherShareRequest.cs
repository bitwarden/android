using System.Collections.Generic;
using System.Linq;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.Request
{
    public class CipherShareRequest
    {
        public CipherShareRequest(Cipher cipher)
        {
            Cipher = new CipherRequest(cipher);
            CollectionIds = cipher.CollectionIds?.ToList();
        }

        public CipherRequest Cipher { get; set; }
        public List<string> CollectionIds { get; set; }
    }
}

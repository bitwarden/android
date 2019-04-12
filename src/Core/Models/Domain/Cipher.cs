using System;
using System.Collections.Generic;
using System.Text;

namespace Bit.Core.Models.Domain
{
    public class Cipher : Domain
    {
        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public CipherString Name { get; set; }
    }
}

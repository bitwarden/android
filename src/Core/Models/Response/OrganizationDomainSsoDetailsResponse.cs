using System;
namespace Bit.Core.Models.Response
{
    public class OrganizationDomainSsoDetailsResponse
    {
        public bool SsoAvailable { get; set; }
        public string DomainName { get; set; }
        public string OrganizationIdentifier { get; set; }
        public bool SsoRequired { get; set; }
        public DateTime? VerifiedDate { get; set; }
    }
}


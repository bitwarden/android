using System;
namespace Bit.Core.Models.Response
{
    public class OrganizationDomainSsoDetailsResponse
    {
        public bool SsoAvailable { get; private set; }
        public string DomainName { get; private set; }
        public string OrganizationIdentifier { get; private set; }
        public bool SsoRequired { get; private set; }
        public DateTime? VerifiedDate { get; private set; }
    }
}


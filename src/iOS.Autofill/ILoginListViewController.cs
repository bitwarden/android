using Bit.iOS.Autofill.Models;

namespace Bit.iOS.Autofill
{
    public interface ILoginListViewController
    {
        Context Context { get; }
        CredentialProviderViewController CPViewController { get; }
    }
}

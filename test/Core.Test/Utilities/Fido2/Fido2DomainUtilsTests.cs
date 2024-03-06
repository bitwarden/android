using Bit.Core.Utilities.Fido2;
using Xunit;

namespace Bit.Core.Test.Utilities.Fido2
{
    public class Fido2DomainUtilsTests
    {
        [Theory]
        // From https://html.spec.whatwg.org/multipage/browsers.html#is-a-registrable-domain-suffix-of-or-is-equal-to
        // [InlineData("0.0.0.0", "0.0.0.0", true)] // IP-addresses not allowed by WebAuthn spec
        // [InlineData("0x10203", "0.1.2.3", true)]
        // [InlineData("[0::1]", "::1", true)]
        [InlineData("example.com", "example.com", true)]
        [InlineData("example.com", "example.com.", false)]
        [InlineData("example.com.", "example.com", false)]
        [InlineData("example.com", "www.example.com", true)]
        [InlineData("com", "example.com", false)]
        [InlineData("example", "example", true)]
        [InlineData("compute.amazonaws.com", "example.compute.amazonaws.com", false)]
        [InlineData("example.compute.amazonaws.com", "www.example.compute.amazonaws.com", false)]
        [InlineData("amazonaws.com", "www.example.compute.amazonaws.com", false)]
        [InlineData("amazonaws.com", "test.amazonaws.com", true)]
        // Overrides by the WebAuthn spec
        [InlineData("0.0.0.0", "0.0.0.0", false)] // IPs not allowed
        [InlineData("0x10203", "0.1.2.3", false)]
        [InlineData("[0::1]", "::1", false)]
        [InlineData("127.0.0.1", "127.0.0.1", false)]
        [InlineData("", "", false)]
        // Custom tests
        [InlineData("sub.login.bitwarden.com", "https://login.bitwarden.com:1337", false)]
        [InlineData("passwordless.dev", "https://login.bitwarden.com:1337", false)]
        [InlineData("login.passwordless.dev", "https://login.bitwarden.com:1337", false)]
        [InlineData("bitwarden", "localhost", false)]
        [InlineData("bitwarden", "bitwarden", true)]
        [InlineData("localhost", "https://localhost:8080", true)]
        [InlineData("bitwarden.com", "https://bitwarden.com", true)]
        [InlineData("bitwarden.com", "https://login.bitwarden.com:1337", true)]
        [InlineData("login.bitwarden.com", "https://login.bitwarden.com:1337", true)]
        [InlineData("login.bitwarden.com", "https://sub.login.bitwarden.com:1337", true)]
        // Origin with trailing slash
        [InlineData("sub.login.bitwarden.com", "https://login.bitwarden.com:1337/", false)]
        [InlineData("passwordless.dev", "https://login.bitwarden.com:1337/", false)]
        [InlineData("login.passwordless.dev", "https://login.bitwarden.com:1337/", false)]
        [InlineData("bitwarden", "localhost/", false)]
        [InlineData("bitwarden", "bitwarden/", true)]
        [InlineData("localhost", "https://localhost:8080/", true)]
        [InlineData("bitwarden.com", "https://bitwarden.com/", true)]
        [InlineData("bitwarden.com", "https://login.bitwarden.com:1337/", true)]
        [InlineData("login.bitwarden.com", "https://login.bitwarden.com:1337/", true)]
        [InlineData("login.bitwarden.com", "https://sub.login.bitwarden.com:1337/", true)]
        public void ValidateRpId(string rpId, string origin, bool isValid)
        {
            Assert.Equal(isValid, Fido2DomainUtils.IsValidRpId(rpId, origin));
        }
    }
}

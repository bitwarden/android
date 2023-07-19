using System;

namespace Bit.Core.Models
{
    public interface ICertificateChainSpec<T, U> : ICertificateChainSpec
    {
        U PrivateKeyRef { get; }
        T LeafCertificate { get; }
        T RootCertificate { get; }
        T[] CertificateChain { get; }
    }

    public interface ICertificateChainSpec : IFormattable
    {
        string Alias { get; }
    }
}

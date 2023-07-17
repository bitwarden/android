using System;

namespace Bit.Core.Models
{
    public interface ICertificateSpec<T, U> : ICertificateSpec
    {
        U PrivateKeyRef { get; }
        T Certificate { get; }
    }

    public interface ICertificateSpec: IFormattable {
        string Alias { get; }
    }
}

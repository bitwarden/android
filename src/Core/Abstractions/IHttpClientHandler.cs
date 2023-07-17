using System.Net.Http;
using Bit.Core.Models;

namespace Bit.Core.Abstractions
{
    public interface IHttpClientHandler
    {
        HttpClientHandler AsClientHandler();

        void UseClientCertificate(ICertificateSpec clientCertificate);
    }
}

using System;
using System.Threading.Tasks;
using Bit.Core.Models;

namespace Bit.Core.Abstractions
{
    public interface ICertificateService
    {
        bool TryRemoveCertificate(string certUri);

        Task<ICertificateChainSpec> GetCertificateAsync(string certUri);

        Task<string> ImportCertificateAsync();

        Task<string> ChooseSystemCertificateAsync();
    }
}

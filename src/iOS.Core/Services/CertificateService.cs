using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models;

namespace Bit.iOS.Core.Services
{
    public class CertificateService : ICertificateService
    {
        public Task<string> ChooseSystemCertificateAsync() => throw new NotImplementedException();
        public Task<ICertificateChainSpec> GetCertificateAsync(string certUri) => throw new NotImplementedException();
        public Task<string> ImportCertificateAsync() => throw new NotImplementedException();
        public bool TryRemoveCertificate(string certUri) => throw new NotImplementedException();
    }
}

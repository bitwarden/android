using System;
using System.Threading.Tasks;
using Bit.Core.Models;

namespace Bit.Core.Abstractions
{
    public interface ICertificateService
    {
        ICertificateSpec GetCertificate(string alias);

        Task<bool> InstallCertificateAsync();

        Task<string> ChooseCertificateAsync(string alias = null);
    }
}

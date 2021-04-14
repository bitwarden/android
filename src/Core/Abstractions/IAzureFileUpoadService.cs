using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IAzureFileUploadService
    {
        Task Upload(string uri, CipherByteArray data, Func<Task<string>> renewalCallback);
    }
}

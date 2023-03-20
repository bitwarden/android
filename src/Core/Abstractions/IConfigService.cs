using System;
using Bit.Core.Models.Response;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IConfigService
    {
        Task<ConfigResponse> GetAllAsync();
    }
}


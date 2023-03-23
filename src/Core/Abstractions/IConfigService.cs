using System;
using System.Threading.Tasks;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IConfigService
    {
        Task<ConfigResponse> GetAllAsync();
    }
}


using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ISettingsApiRepository
    {
        Task<ApiResult<DomainsResponse>> GetDomains(bool excluded = false);
    }
}
using System.Threading.Tasks;
using Bit.App.Models.Api;
using Bit.App.Models.Api.Response;

namespace Bit.App.Abstractions
{
    public interface ISettingsApiRepository
    {
        Task<ApiResult<DomainsReponse>> GetDomains(bool excluded = false);
    }
}
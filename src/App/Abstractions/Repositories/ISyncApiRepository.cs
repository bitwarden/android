using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ISyncApiRepository
    {
        Task<ApiResult<SyncResponse>> Get();
    }
}
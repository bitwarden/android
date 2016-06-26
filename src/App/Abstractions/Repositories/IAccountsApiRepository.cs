using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IAccountsApiRepository
    {
        Task<ApiResult> PostRegisterAsync(RegisterRequest requestObj);
    }
}
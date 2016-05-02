using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IApiService
    {
        HttpClient Client { get; set; }

        Task<ApiResult<T>> HandleErrorAsync<T>(HttpResponseMessage response);
    }
}

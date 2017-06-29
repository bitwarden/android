using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ITwoFactorApiRepository
    {
        Task<ApiResult> PostSendEmailLoginAsync(TwoFactorEmailRequest requestObj);
    }
}
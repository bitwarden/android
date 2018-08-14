using System.Threading.Tasks;
using Bit.App.Models.Api;
using System;

namespace Bit.App.Abstractions
{
    public interface IAccountsApiRepository
    {
        Task<ApiResult<PreloginResponse>> PostPreloginAsync(PreloginRequest requestObj);
        Task<ApiResult> PostRegisterAsync(RegisterRequest requestObj);
        Task<ApiResult> PostPasswordHintAsync(PasswordHintRequest requestObj);
        Task<ApiResult<DateTime?>> GetAccountRevisionDateAsync();
        Task<ApiResult<ProfileResponse>> GetProfileAsync();
    }
}
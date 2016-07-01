using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IApiRepository<TRequest, TResponse, TId>
        where TRequest : class
        where TResponse : class
        where TId : IEquatable<TId>
    {
        Task<ApiResult<TResponse>> GetByIdAsync(TId id);
        Task<ApiResult<ListResponse<TResponse>>> GetAsync();
        Task<ApiResult<TResponse>> PostAsync(TRequest requestObj);
        Task<ApiResult<TResponse>> PutAsync(TId id, TRequest requestObj);
        Task<ApiResult> DeleteAsync(TId id);
    }
}
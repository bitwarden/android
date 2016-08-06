using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Api;
using Bit.App.Repositories;

namespace Bit.App.Abstractions
{
    public interface IDeviceApiRepository : IApiRepository<DeviceRequest, DeviceResponse, string>
    {
        Task<ApiResult> PutTokenAsync(string identifier, DeviceTokenRequest request);
        Task<ApiResult> PutClearTokenAsync(string identifier);
    }
}
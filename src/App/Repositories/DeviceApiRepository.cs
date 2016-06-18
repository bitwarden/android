using System;
using Bit.App.Abstractions;
using Bit.App.Models.Api;

namespace Bit.App.Repositories
{
    public class DeviceApiRepository : ApiRepository<DeviceRequest, DeviceResponse, string>, IDeviceApiRepository
    {
        protected override string ApiRoute => "devices";
    }
}

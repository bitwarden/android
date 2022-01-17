using Bit.Core.Abstractions;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class AppIdService : IAppIdService
    {
        private readonly IStateService _stateService;

        public AppIdService(IStateService stateService)
        {
            _stateService = stateService;
        }

        public async Task<string> GetAppIdAsync()
        {
            var appId = await _stateService.GetAppIdAsync();
            if (appId != null)
            {
                return appId;
            }
            appId = MakeAppId();
            await _stateService.SetAppIdAsync(appId);
            return appId;
        }

        private string MakeAppId()
        {
            return Guid.NewGuid().ToString();
        }
    }
}

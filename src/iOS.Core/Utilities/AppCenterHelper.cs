using Bit.Core.Abstractions;
using System.Threading.Tasks;
using Microsoft.AppCenter;
using Microsoft.AppCenter.Crashes;
using Newtonsoft.Json;

namespace Bit.iOS.Core.Utilities
{
    public class AppCenterHelper
    {
        private const string AppSecret = "51f96ae5-68ba-45f6-99a1-8ad9f63046c3";

        private readonly IAppIdService _appIdService;
        private readonly IStateService _stateService;

        private string _userId;
        private string _appId;

        public AppCenterHelper(
            IAppIdService appIdService,
            IStateService stateService)
        {
            _appIdService = appIdService;
            _stateService = stateService;
        }

        public async Task InitAsync()
        {
            _userId = await _stateService.GetActiveUserIdAsync();
            _appId = await _appIdService.GetAppIdAsync();
            
            AppCenter.Start(AppSecret, typeof(Crashes));
            AppCenter.SetUserId(_userId);
            
            Crashes.GetErrorAttachments = (ErrorReport report) =>
            {
                return new ErrorAttachmentLog[]
                {
                    ErrorAttachmentLog.AttachmentWithText(Description, "crshdesc.txt"),
                };
            };
        }

        public string Description
        {
            get
            {
                return JsonConvert.SerializeObject(new
                {
                    AppId = _appId,
                    UserId = _userId
                }, Formatting.Indented);
            }
        }
    }
}

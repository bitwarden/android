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
        private readonly IUserService _userService;

        private string _userId;
        private string _appId;

        public AppCenterHelper(
            IAppIdService appIdService,
            IUserService userService)
        {
            _appIdService = appIdService;
            _userService = userService;
        }

        public async Task InitAsync()
        {
            _userId = await _userService.GetUserIdAsync();
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

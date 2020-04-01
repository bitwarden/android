#if !FDROID
using Bit.Core.Abstractions;
using System.Threading.Tasks;
using Microsoft.AppCenter;
using Microsoft.AppCenter.Crashes;
using Newtonsoft.Json;

namespace Bit.Droid.Utilities
{
    public class AppCenterHelper
    {
        private const string AppSecret = "d3834185-b4a6-4347-9047-b86c65293d42";

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
#endif

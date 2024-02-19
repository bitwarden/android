#if !FDROID
using System.Diagnostics;
using System.Runtime.CompilerServices;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Microsoft.AppCenter;
using Microsoft.AppCenter.Crashes;
using Newtonsoft.Json;

namespace Bit.Core.Services
{
    public class Logger : ILogger
    {
#if IOS
        private const string AppSecret = "51f96ae5-68ba-45f6-99a1-8ad9f63046c3";
#else
        private const string AppSecret = "d3834185-b4a6-4347-9047-b86c65293d42";
#endif

        private string _userId;
        private string _appId;
        private bool _isInitialised = false;

        static ILogger _instance;
        public static ILogger Instance
        {
            get
            {
                if (_instance is null)
                {
                    _instance = new Logger();
                }
                return _instance;
            }
        }

        protected Logger()
        {
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

        public async Task InitAsync()
        {
            if (_isInitialised)
            {
                return;
            }

            _userId = await ServiceContainer.Resolve<IStateService>().GetActiveUserIdAsync();
            _appId = await ServiceContainer.Resolve<IAppIdService>().GetAppIdAsync();

            AppCenter.Start(AppSecret, typeof(Crashes));

            AppCenter.SetUserId(_userId);

            Crashes.GetErrorAttachments = (ErrorReport report) =>
            {
                return new ErrorAttachmentLog[]
                {
                    ErrorAttachmentLog.AttachmentWithText(Description, "crshdesc.txt"),
                };
            };

            _isInitialised = true;
        }

        public async Task<bool> IsEnabled() => await AppCenter.IsEnabledAsync();

        public async Task SetEnabled(bool value) => await AppCenter.SetEnabledAsync(value);

        public void Error(string message,
                          IDictionary<string, string> extraData = null,
                          [CallerMemberName] string memberName = "",
                          [CallerFilePath] string sourceFilePath = "",
                          [CallerLineNumber] int sourceLineNumber = 0)
        {
            var classAndMethod = $"{Path.GetFileNameWithoutExtension(sourceFilePath)}.{memberName}";
            var filePathAndLineNumber = $"{Path.GetFileName(sourceFilePath)}:{sourceLineNumber}";
            var properties = new Dictionary<string, string>
            {
                ["File"] = filePathAndLineNumber,
                ["Method"] = memberName
            };

            var exception = new Exception(message ?? $"Error found in: {classAndMethod}");
            if (extraData == null)
            {
                Crashes.TrackError(exception, properties);
            }
            else
            {
                var data = properties.Concat(extraData).ToDictionary(x => x.Key, x => x.Value);
                Crashes.TrackError(exception, data);
            }
        }

        public void Exception(Exception exception)
        {
            try
            {
                Crashes.TrackError(exception);
            }
            catch (Exception ex)
            {
                Debug.WriteLine(ex.Message);
            }
        }
    }
}
#endif

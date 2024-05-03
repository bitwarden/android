using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public static class LoggerHelper
    {
        /// <summary>
        /// Logs the exception even if the service can't be resolved.
        /// Useful when we need to log an exception in situations where the ServiceContainer may not be initialized.
        /// </summary>
        /// <param name="ex"></param>
        public static void LogEvenIfCantBeResolved(Exception ex)
        {
            if (ServiceContainer.Resolve<ILogger>("logger", true) is ILogger logger)
            {
                logger.Exception(ex);
            }
            else
            {
#if !FDROID
                // just in case the caller throws the exception in a moment where the logger can't be resolved
                // we need to track the error as well
                Microsoft.AppCenter.Crashes.Crashes.TrackError(ex);
#endif
            }
        }
    }
}

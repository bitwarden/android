using Bit.Core.Abstractions;
using System;

namespace Bit.Core.Services
{
    public class AndroidLogService : INativeLogService
    {
        private static readonly string _tag = "BITWARDEN";

        public void Debug(string message)
        {
            Android.Util.Log.WriteLine(Android.Util.LogPriority.Debug, _tag, message);
        }

        public void Info(string message)
        {
            Android.Util.Log.WriteLine(Android.Util.LogPriority.Info, _tag, message);
        }

        public void Warning(string message)
        {
            Android.Util.Log.WriteLine(Android.Util.LogPriority.Warn, _tag, message);
        }

        public void Error(string message)
        {
            Android.Util.Log.WriteLine(Android.Util.LogPriority.Error, _tag, message);
        }
    }
}

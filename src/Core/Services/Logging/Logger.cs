#if !FDROID
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
using Bit.Core.Abstractions;
using Microsoft.AppCenter.Crashes;

namespace Bit.Core.Services
{
    public class Logger : ILogger
    {
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

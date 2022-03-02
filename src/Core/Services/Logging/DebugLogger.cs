#if !FDROID
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Runtime.CompilerServices;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    public class DebugLogger : ILogger
    {
        static ILogger _instance;
        public static ILogger Instance
        {
            get
            {
                if (_instance is null)
                {
                    _instance = new DebugLogger();
                }
                return _instance;
            }
        }

        protected DebugLogger()
        {
        }

        public void Error(string message, IDictionary<string, string> extraData = null, [CallerMemberName] string memberName = "", [CallerFilePath] string sourceFilePath = "", [CallerLineNumber] int sourceLineNumber = 0)
        {
            var classAndMethod = $"{Path.GetFileNameWithoutExtension(sourceFilePath)}.{memberName}";
            var filePathAndLineNumber = $"{Path.GetFileName(sourceFilePath)}:{sourceLineNumber}";

            if (string.IsNullOrEmpty(message))
            {
                Debug.WriteLine($"Error found in: {classAndMethod})");
                return;
            }

            Debug.WriteLine($"File: {filePathAndLineNumber}");
            Debug.WriteLine($"Method: {memberName}");
            Debug.WriteLine($"Message: {message}");

        }

        public void Exception(Exception ex) => Debug.WriteLine(ex);
    }
}
#endif

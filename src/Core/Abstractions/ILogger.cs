using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;

namespace Bit.Core.Abstractions
{
    public interface ILogger
    {
        /// <summary>
        /// Logs something that is not in itself an exception, e.g. a wrong flow or value that needs to be reported
        /// and looked into.
        /// </summary>
        /// <param name="message">A text to be used as the issue's title</param>
        /// <param name="extraData">Additional data</param>
        void Error(string message,
                   IDictionary<string, string> extraData = null,
                   [CallerMemberName] string memberName = "",
                   [CallerFilePath] string sourceFilePath = "",
                   [CallerLineNumber] int sourceLineNumber = 0);

        /// <summary>
        /// Logs an exception
        /// </summary>
        void Exception(Exception ex);
    }
}

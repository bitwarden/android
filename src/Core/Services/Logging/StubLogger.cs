using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    /// <summary>
    /// A logger that does nothing, this is useful on e.g. FDroid, where we cannot use logging through AppCenter
    /// </summary>
    public class StubLogger : ILogger
    {
        public void Error(string message, IDictionary<string, string> extraData = null, [CallerMemberName] string memberName = "", [CallerFilePath] string sourceFilePath = "", [CallerLineNumber] int sourceLineNumber = 0)
        {
        }

        public void Exception(Exception ex)
        {
        }

        public Task InitAsync() => Task.CompletedTask;

        public Task<bool> IsEnabled() => Task.FromResult(false);

        public Task SetEnabled(bool value) => Task.CompletedTask;
    }
}

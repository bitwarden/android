using Bit.App.Abstractions;
using System.Diagnostics;

namespace Bit.UWP.Services
{
    public class LogService : ILogService
    {
        public void WriteLine(string message) => Debug.WriteLine(message);
    }
}

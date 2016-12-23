using System;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    public class LogService : ILogService
    {
        public void WriteLine(string message)
        {
            Console.WriteLine(message);
        }
    }
}

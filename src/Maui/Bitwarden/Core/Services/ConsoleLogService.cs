using System;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    public class ConsoleLogService : INativeLogService
    {
        public void Debug(string message)
        {
            Console.WriteLine("DEBUG: {0}", message);
        }

        public void Info(string message)
        {
            Console.WriteLine("INFO: {0}", message);
        }

        public void Warning(string message)
        {
            Console.WriteLine("WARNING: {0}", message);
        }

        public void Error(string message)
        {
            Console.WriteLine("ERROR: {0}", message);
        }
    }
}

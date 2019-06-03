namespace Bit.Core.Abstractions
{
    public interface ILogService
    {
        void Debug(string message);
        void Error(string message);
        void Info(string message);
        void Warning(string message);
    }
}
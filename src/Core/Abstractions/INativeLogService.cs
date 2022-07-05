namespace Bit.Core.Abstractions
{
    public interface INativeLogService
    {
        void Debug(string message);
        void Error(string message);
        void Info(string message);
        void Warning(string message);
    }
}

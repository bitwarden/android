namespace Bit.Core.Models.View
{
    public interface ILaunchableView
    {
        bool CanLaunch { get; }
        string LaunchUri { get; }
    }
}

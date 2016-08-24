namespace Bit.App.Abstractions
{
    public interface IDeviceInfoService
    {
        string Model { get; }
        int Version { get; }
    }
}

namespace Bit.App.Abstractions
{
    public interface IDeviceInfoService
    {
        string Model { get; }
        int Version { get; }
        float Scale { get; }
        bool NfcEnabled { get; }
        bool HasCamera { get; }
        bool AutofillServiceSupported { get; }
        bool HasFaceIdSupport { get; }
    }
}

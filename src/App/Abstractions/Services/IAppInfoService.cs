namespace Bit.App.Abstractions
{
    public interface IAppInfoService
    {
        string Build { get; }
        string Version { get; }
        bool AutofillAccessibilityServiceEnabled { get; }
        bool AutofillServiceEnabled { get; }
    }
}

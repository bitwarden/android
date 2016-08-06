namespace Bit.App.Abstractions
{
    public interface IAppIdService
    {
        string AppId { get; }
        string AnonymousAppId { get; }
    }
}

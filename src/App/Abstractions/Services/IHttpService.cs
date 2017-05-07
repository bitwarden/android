namespace Bit.App.Abstractions
{
    public interface IHttpService
    {
        ApiHttpClient ApiClient { get; }
        IdentityHttpClient IdentityClient { get; }
    }
}

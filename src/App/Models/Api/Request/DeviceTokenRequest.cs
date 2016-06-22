namespace Bit.App.Models.Api
{
    public class DeviceTokenRequest
    {
        public DeviceTokenRequest(string token)
        {
            PushToken = token;
        }

        public string PushToken { get; set; }
    }
}

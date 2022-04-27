using Xamarin.UITest;

namespace Bit.UITests.Helpers
{
    public static class AppState
    {

        public static void EnableScreenshots(this IApp app)
        {
            //TODO placeholder, mobile app needs the service / setting to enable Android screenshots first
            app.Invoke("Zamboni");
        }
    }
}

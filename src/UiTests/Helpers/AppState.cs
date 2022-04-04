using Xamarin.UITest;

namespace Bit.UITests.Helpers
{
    public static class AppState
    {
        public static void CallBackdoor(this IApp app, string paramExample)
        {
            var args = new object[]
            {
                paramExample,
            };

            app.Invoke("Zamboni", args);
        }
    }
}

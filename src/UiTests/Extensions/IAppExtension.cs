using System;
using Xamarin.UITest;
using Xamarin.UITest.Queries;

namespace Bit.UITests.Extensions
{
    public static class IAppExtension
    {
        public static void Wait(this IApp app, float seconds)
        {
            var waitTime = DateTime.Now + TimeSpan.FromSeconds(seconds);

            app.WaitFor(() => DateTime.Now > waitTime);
        }

        public static void WaitAndTapElement(this IApp app, Func<AppQuery, AppQuery> elementQuery)
        {
            app.WaitForElement(elementQuery);
            app.Tap(elementQuery);
        }

        public static void WaitAndTapElement(this IApp app, Func<AppQuery, AppWebQuery> elementQuery)
        {
            app.WaitForElement(elementQuery);
            app.Tap(elementQuery);
        }
    }
}

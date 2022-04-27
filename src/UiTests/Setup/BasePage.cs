using System;
using Bit.UITests.Extensions;
using Bit.UITests.Helpers;
using NUnit.Framework;
using Xamarin.UITest;
using Query = System.Func<Xamarin.UITest.Queries.AppQuery, Xamarin.UITest.Queries.AppQuery>;

namespace Bit.UITests.Setup
{
    public abstract class BasePage
    {

        protected BasePage()
        {
            AssertOnPage(CustomWaitTimes.DefaultCustomTimeout);
            App.Screenshot("On " + GetType().Name);
        }

        protected readonly Query LoadingIndicator = x => x.Marked("activity_indicator");

        protected IApp App => AppManager.App;

        protected bool OnAndroid => AppManager.Platform == Platform.Android;

        // ReSharper disable once InconsistentNaming
        protected bool OniOS => AppManager.Platform == Platform.iOS;

        protected abstract PlatformQuery Trait { get; }

        /// <summary>
        /// Verifies that the trait is still present. Defaults to no wait.
        /// </summary>
        /// <param name="timeout">Time to wait before the assertion fails</param>
        public void AssertOnPage(TimeSpan? timeout = default(TimeSpan?))
        {
            var message = "Unable to verify on page: " + GetType().Name;

            if (timeout == null)
            {
                Assert.IsNotEmpty(App.Query(Trait.Current), message);
            }
            else
            {
                Assert.DoesNotThrow(() => App.WaitForElement(Trait.Current, timeout: timeout), message);
            }
        }

        /// <summary>
        /// Verifies that the trait is no longer present. Defaults to a 5 second wait.
        /// </summary>
        /// <param name="timeout">Time to wait before the assertion fails</param>
        public void WaitForPageToLeave(TimeSpan? timeout = default(TimeSpan?))
        {
            timeout ??= TimeSpan.FromSeconds(5);
            var message = "Unable to verify *not* on page: " + GetType().Name;

            Assert.DoesNotThrow(() => App.WaitForNoElement(Trait.Current, timeout: timeout), message);
        }


        public BasePage Wait(int seconds)
        {
            App.Wait(seconds);
            return this;
        }

        public BasePage Back()
        {
            App.Back();
            return this;
        }
    }
}

using System;
using Xamarin.UITest.Utils;

namespace Bit.UITests.Helpers
{
    public class CustomWaitTimes : IWaitTimes
    {
        private readonly TimeSpan _timeout;
        public static readonly TimeSpan DefaultCustomTimeout = TimeSpan.FromSeconds(30);

        public CustomWaitTimes()
        {
            _timeout = DefaultCustomTimeout;
        }

        public CustomWaitTimes(TimeSpan timeoutTimeSpan)
        {
            _timeout = timeoutTimeSpan;
        }

        public TimeSpan GestureCompletionTimeout => _timeout;

        public TimeSpan GestureWaitTimeout => _timeout;

        public TimeSpan WaitForTimeout => _timeout;
    }
}

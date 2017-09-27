using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.UWP.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        public void Dispatch(Action completionHandler = null)
        {
            throw new NotImplementedException();
        }

        public void SetAppOptOut(bool optOut)
        {
            throw new NotImplementedException();
        }

        public void TrackAppEvent(string eventName, string label = null)
        {
            throw new NotImplementedException();
        }

        public void TrackEvent(string category, string eventName, string label = null)
        {
            throw new NotImplementedException();
        }

        public void TrackException(string message, bool fatal)
        {
            throw new NotImplementedException();
        }

        public void TrackExtensionEvent(string eventName, string label = null)
        {
            throw new NotImplementedException();
        }

        public void TrackPage(string pageName)
        {
            throw new NotImplementedException();
        }
    }
}

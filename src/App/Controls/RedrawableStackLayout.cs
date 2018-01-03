using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class RedrawableStackLayout : StackLayout
    {
        private DateTime _lastRedraw = DateTime.MinValue;
        private TimeSpan _redrawThreshold = TimeSpan.FromMilliseconds(1000);

        public async Task RedrawIfNeededAsync(int delay = 0, bool force = false)
        {
            var now = DateTime.UtcNow;
            if(Device.RuntimePlatform == Device.iOS && (force || (now - _lastRedraw) > _redrawThreshold))
            {
                _lastRedraw = now;
                if(delay > 0)
                {
                    await Task.Delay(delay);
                }

                InvalidateLayout();
            }
        }
    }
}

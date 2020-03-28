using Android.Views.Accessibility;
using System;
using System.Collections.Generic;

namespace Bit.Droid.Accessibility
{
    public class NodeList : List<AccessibilityNodeInfo>, IDisposable
    {
        public void Dispose()
        {
            foreach (var item in this)
            {
                item.Recycle();
                item.Dispose();
            }
        }
    }
}

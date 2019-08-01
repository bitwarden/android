using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Widget;

namespace Bit.Droid.Utilities
{
    public static class StaticStore
    {
        public static string LastClipboardValue { get; set; }
    }
}

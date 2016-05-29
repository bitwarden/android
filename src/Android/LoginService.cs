using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Android.AccessibilityServices;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Views.Accessibility;
using Android.Widget;

namespace Bit.Android
{
    [Service(Permission = "android.permission.BIND_ACCESSIBILITY_SERVICE", Label = "bitwarden")]
    [IntentFilter(new string[] { "android.accessibilityservice.AccessibilityService" })]
    [MetaData("android.accessibilityservice", Resource = "@xml/accessibilityservice")]
    public class LoginService : AccessibilityService
    {
        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            var eventType = e.EventType;
            switch(eventType)
            {
                case EventTypes.ViewTextSelectionChanged:
                    if(e.Source.Password)
                    {
                        var bundle = new Bundle();
                        bundle.PutCharSequence(AccessibilityNodeInfo.ActionArgumentSetTextCharsequence, "mypassword");
                        e.Source.PerformAction(global::Android.Views.Accessibility.Action.SetText, bundle);
                    }
                    break;
                default:
                    break;
            }
        }

        public override void OnInterrupt()
        {

        }
    }
}
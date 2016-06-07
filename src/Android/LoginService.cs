using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Android.AccessibilityServices;
using Android.App;
using Android.Content;
using Android.Graphics;
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
        private View mView;

        private WindowManagerLayoutParams mParams;
        private IWindowManager mWindowManager;

        public override void OnAccessibilityEvent(AccessibilityEvent e)
        {
            var eventType = e.EventType;
            switch(eventType)
            {
                case EventTypes.ViewTextSelectionChanged:
                    if(e.Source.Password && string.IsNullOrWhiteSpace(e.Source.Text))
                    {
                        MakeWindow();
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

        public void MakeWindow()
        {
            mView = new MyLoadView(this);

            mParams = new WindowManagerLayoutParams(
                    WindowManagerTypes.SystemOverlay,
                    WindowManagerFlags.WatchOutsideTouch,
                    Format.Translucent);

            mParams.Gravity = GravityFlags.Top | GravityFlags.Right;
            mParams.Title = "Test window";

            mWindowManager = GetSystemService(WindowService).JavaCast<IWindowManager>();
            mWindowManager.AddView(mView, mParams);
        }

        public class MyLoadView : View
        {
            private Paint mPaint;

            public MyLoadView(Context context)
                : base(context)
            {
                mPaint = new Paint();
                mPaint.TextSize = 50;
                mPaint.SetARGB(200, 200, 200, 200);
            }

            protected override void OnDraw(Canvas canvas)
            {
                base.OnDraw(canvas);
                canvas.DrawText("test test test", 0, 100, mPaint);
            }
        }
    }
}
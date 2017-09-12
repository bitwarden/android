using System;
using System.ComponentModel;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Android.Text.Method;
using Android.Views;

[assembly: ExportRenderer(typeof(ExtendedEditor), typeof(ExtendedEditorRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedEditorRenderer : EditorRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Editor> e)
        {
            base.OnElementChanged(e);

            var view = (ExtendedEditor)Element;

            SetBorder(view);
            SetScrollable();
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var view = (ExtendedEditor)Element;

            if(e.PropertyName == ExtendedEditor.HasBorderProperty.PropertyName)
            {
                SetBorder(view);
            }
            else
            {
                base.OnElementPropertyChanged(sender, e);
                if(e.PropertyName == VisualElement.BackgroundColorProperty.PropertyName)
                {
                    Control.SetBackgroundColor(view.BackgroundColor.ToAndroid());
                }
            }
        }

        private void SetBorder(ExtendedEditor view)
        {
            if(!view.HasBorder)
            {
                Control.SetBackgroundColor(global::Android.Graphics.Color.Transparent);
            }
        }

        private void SetScrollable()
        {
            // While scrolling inside Editor stop scrolling parent view.
            Control.OverScrollMode = OverScrollMode.Always;
            Control.ScrollBarStyle = ScrollbarStyles.InsideInset;
            Control.SetOnTouchListener(new EditorTouchListener());

            // For Scrolling in Editor innner area
            Control.VerticalScrollBarEnabled = true;
            Control.MovementMethod = ScrollingMovementMethod.Instance;
            Control.ScrollBarStyle = ScrollbarStyles.InsideInset;

            // Force scrollbars to be displayed
            var arr = Control.Context.Theme.ObtainStyledAttributes(new int[0]);
            InitializeScrollbars(arr);
            arr.Recycle();
        }

        public class EditorTouchListener : Java.Lang.Object, IOnTouchListener
        {
            public bool OnTouch(global::Android.Views.View v, MotionEvent e)
            {
                v.Parent?.RequestDisallowInterceptTouchEvent(true);
                if((e.Action & MotionEventActions.Up) != 0 && (e.ActionMasked & MotionEventActions.Up) != 0)
                {
                    v.Parent?.RequestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        }
    }
}

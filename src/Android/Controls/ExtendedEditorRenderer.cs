using System;
using System.ComponentModel;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

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
    }
}

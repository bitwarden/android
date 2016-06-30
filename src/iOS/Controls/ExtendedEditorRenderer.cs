using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedEditor), typeof(ExtendedEditorRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedEditorRenderer : EditorRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Editor> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as ExtendedEditor;
            if(view != null)
            {
                var descriptor = UIFontDescriptor.PreferredBody;
                Control.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
            }
        }
    }
}

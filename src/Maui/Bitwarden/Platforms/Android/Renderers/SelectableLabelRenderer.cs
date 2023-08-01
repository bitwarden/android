using System;
using Android.Content;
using Bit.App.Controls;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class SelectableLabelRenderer : LabelRenderer
    {
        public SelectableLabelRenderer(Context context) : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);

            if (Control != null)
            {
                Control.SetTextIsSelectable(true);
            }
        }
    }
}

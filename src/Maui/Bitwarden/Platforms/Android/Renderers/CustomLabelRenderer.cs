using System.ComponentModel;
using Android.Content;
using Android.OS;
using Bit.App.Controls;
using Bit.App.Droid.Renderers;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class CustomLabelRenderer : LabelRenderer
    {
        public CustomLabelRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);

            if (Control != null && e.NewElement is CustomLabel label)
            {
                if (label.FontWeight.HasValue && Build.VERSION.SdkInt >= BuildVersionCodes.P)
                {
                    Control.Typeface = Android.Graphics.Typeface.Create(null, label.FontWeight.Value, false);
                }
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var label = sender as CustomLabel;
            switch (e.PropertyName)
            {
                case nameof(CustomLabel.AutomationId):
                    Control.ContentDescription = label.AutomationId;
                    break;
            }
            base.OnElementPropertyChanged(sender, e);
        }
    }
}

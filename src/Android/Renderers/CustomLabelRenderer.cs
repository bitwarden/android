using System.ComponentModel;
using Android.Content;
using Android.OS;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(CustomLabel), typeof(CustomLabelRenderer))]
namespace Bit.Droid.Renderers
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

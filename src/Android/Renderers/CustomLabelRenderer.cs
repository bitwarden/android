using System;
using Bit.App.Controls;
using System.ComponentModel;
using Xamarin.Forms.Platform.Android;
using Android.Content;
using Xamarin.Forms;
using Bit.Droid.Renderers;

[assembly: ExportRenderer(typeof(CustomLabel), typeof(CustomLabelRenderer))]
namespace Bit.Droid.Renderers
{
    public class CustomLabelRenderer : LabelRenderer
    {
        public CustomLabelRenderer(Context context)
            : base(context)
        { }

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


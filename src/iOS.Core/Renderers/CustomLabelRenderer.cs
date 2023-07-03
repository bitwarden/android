using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using Bit.iOS.Core.Utilities;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(Label), typeof(CustomLabelRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class CustomLabelRenderer : LabelRenderer
	{
		protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
		{
			base.OnElementChanged(e);
			if (Control != null && e.NewElement is Label)
			{
				UpdateFont();
			}
		}

		protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
		{
            var label = sender as CustomLabel;
            switch (e.PropertyName)
            {
                case nameof(CustomLabel.AutomationId):
                    Control.AccessibilityIdentifier = label.AutomationId;
                    break;
            }

            base.OnElementPropertyChanged(sender, e);

			if (e.PropertyName == Label.FontProperty.PropertyName ||
				e.PropertyName == Label.TextProperty.PropertyName ||
				e.PropertyName == Label.FormattedTextProperty.PropertyName)
			{
				UpdateFont();
			}
		}

		private void UpdateFont()
		{
			if (Element is null || Control is null)
				return;

            var pointSize = iOSHelpers.GetAccessibleFont<Label>(Element.FontSize);
			if (pointSize != null)
			{
				Control.Font = UIFont.FromDescriptor(Element.ToUIFont().FontDescriptor, pointSize.Value);
			}
			// TODO: For now, I'm only doing this for IconLabel with setup just in case I break the whole app labels.
			// We need to revisit this when we address Accessibility Large Font issues across the app
            // to check if we can left it more generic like
			// else if (Element.FontFamily != null)
			else if (Element is IconLabel iconLabel && iconLabel.ShouldUpdateFontSizeDynamicallyForAccesibility)
			{
				var customFont = Element.ToUIFont();
				Control.Font = new UIFontMetrics(UIFontTextStyle.Body.GetConstant()).GetScaledFont(customFont);
			}
		}
	}
}

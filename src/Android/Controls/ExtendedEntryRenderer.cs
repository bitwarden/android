using System;
using System.ComponentModel;
using Android.Graphics;
using Android.Text;
using Android.Text.Method;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedEntry), typeof(ExtendedEntryRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedEntryRenderer : EntryRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Entry> e)
        {
            base.OnElementChanged(e);

            var view = (ExtendedEntry)Element;

            if(Control != null && e.NewElement != null && e.NewElement.IsPassword)
            {
                Control.SetTypeface(Typeface.Default, TypefaceStyle.Normal);
                Control.TransformationMethod = new PasswordTransformationMethod();
            }

            SetBorder(view);
            SetPlaceholderTextColor(view);
            SetMaxLength(view);
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var view = (ExtendedEntry)Element;

            if(e.PropertyName == ExtendedEntry.HasBorderProperty.PropertyName
                || e.PropertyName == ExtendedEntry.HasOnlyBottomBorderProperty.PropertyName
                || e.PropertyName == ExtendedEntry.BorderColorProperty.PropertyName)
            {
                SetBorder(view);
            }
            if(e.PropertyName == ExtendedEntry.PlaceholderTextColorProperty.PropertyName)
            {
                SetPlaceholderTextColor(view);
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

        private void SetBorder(ExtendedEntry view)
        {
            //Not suported on Android
        }

        private void SetMaxLength(ExtendedEntry view)
        {
            Control.SetFilters(new IInputFilter[] { new InputFilterLengthFilter(view.MaxLength) });
        }

        private void SetPlaceholderTextColor(ExtendedEntry view)
        {
            if(view.PlaceholderTextColor != Xamarin.Forms.Color.Default)
            {
                Control.SetHintTextColor(view.PlaceholderTextColor.ToAndroid());
            }
        }
    }
}

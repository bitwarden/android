#if ANDROID
using System;
using System.Collections.Specialized;
using Android.App;
using Android.Content.PM;
using Android.Content.Res;
using Android.Graphics.Drawables;
using Android.Text;
using Android.Text.Style;
using Android.Widget;
using Microsoft.Maui;
using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;
using AGravityFlags = Android.Views.GravityFlags;
using ALayoutDirection = Android.Views.LayoutDirection;
using AppCompatAlertDialog = AndroidX.AppCompat.App.AlertDialog;
using AResource = Android.Resource;
using ATextAlignment = Android.Views.TextAlignment;
using ATextDirection = Android.Views.TextDirection;

namespace Bit.Core.Controls.Picker
{
    // HACK: Due to https://github.com/dotnet/maui/issues/19681 and not willing to use reflection to access
    // the alert dialog, we need to redefine the PickerHandler implementation for a custom one of ours
    // which handles showing the current selected item. Remove this workaround when MAUI releases a fix for this.
    // This is an adapted copy from https://github.com/dotnet/maui/blob/main/src/Core/src/Handlers/Picker/PickerHandler.Android.cs
    public partial class PickerHandler : ViewHandler<IPicker, MauiPicker>
	{
		AppCompatAlertDialog? _dialog;

		protected override MauiPicker CreatePlatformView() =>
			new MauiPicker(Context);

		protected override void ConnectHandler(MauiPicker platformView)
		{
			platformView.FocusChange += OnFocusChange;
			platformView.Click += OnClick;

			base.ConnectHandler(platformView);
		}

		protected override void DisconnectHandler(MauiPicker platformView)
		{
			platformView.FocusChange -= OnFocusChange;
			platformView.Click -= OnClick;

			base.DisconnectHandler(platformView);
		}

		// This is a Android-specific mapping
		public static void MapBackground(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateBackground(picker);
		}

		// TODO Uncomment me on NET8 [Obsolete]
		public static void MapReload(IPickerHandler handler, IPicker picker, object? args) => Reload(handler);

		internal static void MapItems(IPickerHandler handler, IPicker picker) => Reload(handler);

		public static void MapTitle(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateTitle(picker);
		}

		public static void MapTitleColor(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateTitleColor(picker);
		}

		public static void MapSelectedIndex(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateSelectedIndex(picker);
		}

		public static void MapCharacterSpacing(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateCharacterSpacing(picker);
		}

		public static void MapFont(IPickerHandler handler, IPicker picker)
		{
			var fontManager = handler.GetRequiredService<IFontManager>();

			handler.PlatformView?.UpdateFont(picker, fontManager);
		}

		public static void MapHorizontalTextAlignment(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateHorizontalAlignment(picker.HorizontalTextAlignment);
		}

		public static void MapTextColor(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView.UpdateTextColor(picker);
		}

		public static void MapVerticalTextAlignment(IPickerHandler handler, IPicker picker)
		{
			handler.PlatformView?.UpdateVerticalAlignment(picker.VerticalTextAlignment);
		}

		void OnFocusChange(object? sender, global::Android.Views.View.FocusChangeEventArgs e)
		{
			if (PlatformView == null)
				return;

			if (e.HasFocus)
			{
				if (PlatformView.Clickable)
					PlatformView.CallOnClick();
				else
					OnClick(PlatformView, EventArgs.Empty);
			}
			else if (_dialog != null)
			{
				_dialog.Hide();
				_dialog = null;
			}
		}

		void OnClick(object? sender, EventArgs e)
		{
			if (_dialog == null && VirtualView != null)
			{
				using (var builder = new AppCompatAlertDialog.Builder(Context))
				{
					if (VirtualView.TitleColor == null)
					{
						builder.SetTitle(VirtualView.Title ?? string.Empty);
					}
					else
					{
						var title = new SpannableString(VirtualView.Title ?? string.Empty);
#pragma warning disable CA1416 // https://github.com/xamarin/xamarin-android/issues/6962
						title.SetSpan(new ForegroundColorSpan(VirtualView.TitleColor.ToPlatform()), 0, title.Length(), SpanTypes.ExclusiveExclusive);
#pragma warning restore CA1416
						builder.SetTitle(title);
					}

					string[] items = VirtualView.GetItemsAsArray();

					for (var i = 0; i < items.Length; i++)
					{
						var item = items[i];
						if (item == null)
							items[i] = String.Empty;
					}

					builder.SetSingleChoiceItems(items, VirtualView.SelectedIndex, (s, e) =>
					{
						var selectedIndex = e.Which;
						VirtualView.SelectedIndex = selectedIndex;
						base.PlatformView?.UpdatePicker(VirtualView);
						_dialog.Dismiss();
					});

					builder.SetNegativeButton(AResource.String.Cancel, (o, args) => { });

					_dialog = builder.Create();
				}

				if (_dialog == null)
					return;

				_dialog.UpdateFlowDirection(PlatformView);

				_dialog.SetCanceledOnTouchOutside(true);

				_dialog.DismissEvent += (sender, args) =>
				{
					_dialog = null;
				};

				_dialog.Show();
			}
		}

		static void Reload(IPickerHandler handler)
		{
			handler.PlatformView.UpdatePicker(handler.VirtualView);
		}
	}

	public static class PickerExtensions
	{
        const AGravityFlags HorizontalGravityMask = AGravityFlags.CenterHorizontal | AGravityFlags.End | AGravityFlags.Start;

        internal static void UpdatePicker(this MauiPicker platformPicker, IPicker picker)
		{
			platformPicker.Hint = picker.Title;

			if (picker.SelectedIndex == -1 || picker.SelectedIndex >= picker.GetCount())
				platformPicker.Text = null;
			else
				platformPicker.Text = picker.GetItem(picker.SelectedIndex);
		}

        internal static void UpdateHorizontalAlignment(this EditText view, TextAlignment alignment, AGravityFlags orMask = AGravityFlags.NoGravity)
        {
            if (!Rtl.IsSupported)
            {
                view.Gravity = (view.Gravity & ~HorizontalGravityMask) | alignment.ToHorizontalGravityFlags() | orMask;
            }
            else
                view.TextAlignment = alignment.ToTextAlignment();
        }

        internal static AGravityFlags ToHorizontalGravityFlags(this TextAlignment alignment)
        {
            switch (alignment)
            {
                case TextAlignment.Center:
                    return AGravityFlags.CenterHorizontal;
                case TextAlignment.End:
                    return AGravityFlags.End;
                default:
                    return AGravityFlags.Start;
            }
        }

        internal static ATextAlignment ToTextAlignment(this TextAlignment alignment)
        {
            switch (alignment)
            {
                case TextAlignment.Center:
                    return ATextAlignment.Center;
                case TextAlignment.End:
                    return ATextAlignment.ViewEnd;
                default:
                    return ATextAlignment.ViewStart;
            }
        }

        internal static void UpdateFlowDirection(this AndroidX.AppCompat.App.AlertDialog alertDialog, MauiPicker platformPicker)
        {
            var platformLayoutDirection = platformPicker.LayoutDirection;

            // Propagate the MauiPicker LayoutDirection to the AlertDialog
            var dv = alertDialog.Window?.DecorView;

            if (dv is not null)
                dv.LayoutDirection = platformLayoutDirection;

            var lv = alertDialog?.ListView;

            if (lv is not null)
            {
                lv.LayoutDirection = platformLayoutDirection;
                lv.TextDirection = platformLayoutDirection.ToTextDirection();
            }
        }

        internal static ATextDirection ToTextDirection(this ALayoutDirection direction)
        {
            switch (direction)
            {
                case ALayoutDirection.Ltr:
                    return ATextDirection.Ltr;
                case ALayoutDirection.Rtl:
                    return ATextDirection.Rtl;
                default:
                    return ATextDirection.Inherit;
            }
        }

        public static T GetRequiredService<T>(this IElementHandler handler)
            where T : notnull
        {
            var services = handler.GetServiceProvider();

            var service = services.GetRequiredService<T>();

            return service;
        }

        public static IServiceProvider GetServiceProvider(this IElementHandler handler)
        {
            var context = handler.MauiContext ??
                throw new InvalidOperationException($"Unable to find the context. The {nameof(ElementHandler.MauiContext)} property should have been set by the host.");

            var services = context?.Services ??
                throw new InvalidOperationException($"Unable to find the service provider. The {nameof(ElementHandler.MauiContext)} property should have been set by the host.");

            return services;
        }
    }

    static class Rtl
    {
        /// <summary>
        /// True if /manifest/application@android:supportsRtl="true"
        /// </summary>
        public static readonly bool IsSupported =
            (Android.App.Application.Context?.ApplicationInfo?.Flags & ApplicationInfoFlags.SupportsRtl) != 0;
    }

}
#endif
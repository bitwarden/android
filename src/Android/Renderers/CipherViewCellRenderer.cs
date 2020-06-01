using Android.App;
using Android.Content;
using Android.Graphics;
using Android.Runtime;
using Android.Util;
using Android.Views;
using Android.Views.InputMethods;
using Android.Widget;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Droid.Renderers;
using FFImageLoading;
using FFImageLoading.Views;
using FFImageLoading.Work;
using System;
using System.ComponentModel;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(CipherViewCell), typeof(CipherViewCellRenderer))]
namespace Bit.Droid.Renderers
{
    public class CipherViewCellRenderer : ViewRenderer<CipherViewCell, AndroidCipherCell>
    {
        private static Typeface _faTypeface;
        private static Typeface _miTypeface;
        private static Android.Graphics.Color _textColor;
        private static Android.Graphics.Color _mutedColor;
        private static Android.Graphics.Color _disabledIconColor;

        public CipherViewCellRenderer(Context context) : base(context) { }

        protected override void OnElementChanged (ElementChangedEventArgs<CipherViewCell> e)
        {
            base.OnElementChanged(e);
            if (e.OldElement != null)
            {
                e.OldElement.PropertyChanged -= CellPropertyChanged;
            }
            if (e.NewElement == null)
            {
                return;
            }

            if (_faTypeface == null)
            {
                _faTypeface = Typeface.CreateFromAsset(Context.Assets, "FontAwesome.ttf");
            }
            if (_miTypeface == null)
            {
                _miTypeface = Typeface.CreateFromAsset(Context.Assets, "MaterialIcons_Regular.ttf");
            }
            if (_textColor == default)
            {
                _textColor = ThemeManager.GetResourceColor("TextColor").ToAndroid();
            }
            if (_mutedColor == default)
            {
                _mutedColor = ThemeManager.GetResourceColor("MutedColor").ToAndroid();
            }
            if (_disabledIconColor == default)
            {
                _disabledIconColor = ThemeManager.GetResourceColor("DisabledIconColor").ToAndroid();
            }

            if (Control == null)
            {
                if (Control == null)
                {
                    SetNativeControl(new AndroidCipherCell(Context, e.NewElement, _faTypeface, _miTypeface));
                    Control.Element.PropertyChanged += CellPropertyChanged;
                }
            }

            Control.UpdateCell(Element);
            Control.UpdateColors(_textColor, _mutedColor, _disabledIconColor);
        }

        public void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var cipherCell = sender as CipherViewCell;
            if (cipherCell == null)
            {
                return;
            }
            Control.CipherViewCell = cipherCell;
            if (e.PropertyName == CipherViewCell.CipherProperty.PropertyName)
            {
                Control.UpdateCell(cipherCell);
            }
            else if (e.PropertyName == CipherViewCell.WebsiteIconsEnabledProperty.PropertyName)
            {
                Control.UpdateIconImage(cipherCell);
            }
        }
    }

    public class AndroidCipherCell : LinearLayout, INativeElementView
    {
        private readonly Typeface _faTypeface;
        private readonly Typeface _miTypeface;

        private IScheduledWork _currentTask;

        public AndroidCipherCell(Context context, CipherViewCell cipherView, Typeface faTypeface, Typeface miTypeface)
            : base(context)
        {
            CipherViewCell = cipherView;
            _faTypeface = faTypeface;
            _miTypeface = miTypeface;

            var view = context.GetActivity().LayoutInflater.Inflate(Resource.Layout.CipherViewCell, null);
            IconImage = view.FindViewById<IconImageView>(Resource.Id.CipherCellIconImage);
            Icon = view.FindViewById<TextView>(Resource.Id.CipherCellIcon);
            Name = view.FindViewById<TextView>(Resource.Id.CipherCellName);
            SubTitle = view.FindViewById<TextView>(Resource.Id.CipherCellSubTitle);
            SharedIcon = view.FindViewById<TextView>(Resource.Id.CipherCellSharedIcon);
            AttachmentsIcon = view.FindViewById<TextView>(Resource.Id.CipherCellAttachmentsIcon);
            MoreButton = view.FindViewById<Android.Widget.Button>(Resource.Id.CipherCellButton);
            MoreButton.Click += MoreButton_Click;

            Icon.Typeface = _faTypeface;
            SharedIcon.Typeface = _faTypeface;
            AttachmentsIcon.Typeface = _faTypeface;
            MoreButton.Typeface = _miTypeface;

            var small = (float)Device.GetNamedSize(NamedSize.Small, typeof(Label));
            Icon.SetTextSize(ComplexUnitType.Pt, 10);
            Name.SetTextSize(ComplexUnitType.Sp, (float)Device.GetNamedSize(NamedSize.Medium, typeof(Label)));
            SubTitle.SetTextSize(ComplexUnitType.Sp, small);
            SharedIcon.SetTextSize(ComplexUnitType.Sp, small);
            AttachmentsIcon.SetTextSize(ComplexUnitType.Sp, small);
            MoreButton.SetTextSize(ComplexUnitType.Sp, 25);

            AddView(view);
        }

        public CipherViewCell CipherViewCell { get; set; }
        public Element Element => CipherViewCell;

        public IconImageView IconImage { get; set; }
        public TextView Icon { get; set; }
        public TextView Name { get; set; }
        public TextView SubTitle { get; set; }
        public TextView SharedIcon { get; set; }
        public TextView AttachmentsIcon { get; set; }
        public Android.Widget.Button MoreButton { get; set; }

        public void UpdateCell(CipherViewCell cipherCell)
        {
            var cipher = cipherCell.Cipher;
            if (cipher == null)
            {
                return;
            }

            UpdateIconImage(cipherCell);

            Name.Text = cipher.Name;
            if (!string.IsNullOrWhiteSpace(cipher.SubTitle))
            {
                SubTitle.Text = cipher.SubTitle;
                SubTitle.Visibility = ViewStates.Visible;
            }
            else
            {
                SubTitle.Visibility = ViewStates.Invisible;
            }
            SharedIcon.Visibility = cipher.Shared ? ViewStates.Visible : ViewStates.Gone;
            AttachmentsIcon.Visibility = cipher.HasAttachments ? ViewStates.Visible : ViewStates.Gone;
        }

        public void UpdateIconImage(CipherViewCell cipherCell)
        {
            if (_currentTask != null && !_currentTask.IsCancelled && !_currentTask.IsCompleted)
            {
                _currentTask.Cancel();
            }

            var cipher = cipherCell.Cipher;

            var iconImage = cipherCell.GetIconImage(cipher);
            if (iconImage.Item2 != null)
            {
                IconImage.SetImageResource(Resource.Drawable.login);
                IconImage.Visibility = ViewStates.Visible;
                Icon.Visibility = ViewStates.Gone;
                _currentTask = ImageService.Instance.LoadUrl(iconImage.Item2).DownSample(64).Into(IconImage);
                IconImage.Key = iconImage.Item2;
            }
            else
            {
                IconImage.Visibility = ViewStates.Gone;
                Icon.Visibility = ViewStates.Visible;
                Icon.Text = iconImage.Item1;
            }
        }

        public void UpdateColors(Android.Graphics.Color textColor, Android.Graphics.Color mutedColor,
            Android.Graphics.Color iconDisabledColor)
        {
            Name.SetTextColor(textColor);
            SubTitle.SetTextColor(mutedColor);
            Icon.SetTextColor(mutedColor);
            SharedIcon.SetTextColor(mutedColor);
            AttachmentsIcon.SetTextColor(mutedColor);
            MoreButton.SetTextColor(iconDisabledColor);
        }

        private void MoreButton_Click(object sender, EventArgs e)
        {
            if (CipherViewCell.ButtonCommand?.CanExecute(CipherViewCell.Cipher) ?? false)
            {
                CipherViewCell.ButtonCommand.Execute(CipherViewCell.Cipher);
            }
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                MoreButton.Click -= MoreButton_Click;
            }
            base.Dispose(disposing);
        }
    }

    [Android.Runtime.Preserve(AllMembers = true)]
    [Register("bit.droid.renderers.IconImageView")]
    public class IconImageView : ImageView
    {
        public IconImageView(Context context) : base(context)
        { }

        public IconImageView(IntPtr javaReference, JniHandleOwnership transfer)
            : base(javaReference, transfer)
        { }

        public IconImageView(Context context, IAttributeSet attrs)
            : base(context, attrs)
        { }

        public string Key { get; set; }

        protected override void JavaFinalize()
        {
            SetImageDrawable(null);
            SetImageBitmap(null);
            ImageService.Instance.InvalidateCacheEntryAsync(Key, FFImageLoading.Cache.CacheType.Memory);
            base.JavaFinalize();
        }
    }
}

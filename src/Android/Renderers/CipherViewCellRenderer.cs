using Android.App;
using Android.Content;
using Android.Graphics;
using Android.Runtime;
using Android.Util;
using Android.Views;
using Android.Views.InputMethods;
using Android.Widget;
using Bit.App.Controls;
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
    public class CipherViewCellRenderer : ViewCellRenderer
    {
        private static Typeface _faTypeface;
        private static Typeface _miTypeface;
        private static Android.Graphics.Color _textColor;
        private static Android.Graphics.Color _mutedColor;
        private static Android.Graphics.Color _disabledIconColor;

        private AndroidCipherCell _cell;

        protected override Android.Views.View GetCellCore(Cell item, Android.Views.View convertView,
            ViewGroup parent, Context context)
        {
            if (_faTypeface == null)
            {
                _faTypeface = Typeface.CreateFromAsset(context.Assets, "FontAwesome.ttf");
            }
            if (_miTypeface == null)
            {
                _miTypeface = Typeface.CreateFromAsset(context.Assets, "MaterialIcons_Regular.ttf");
            }
            if (_textColor == default(Android.Graphics.Color))
            {
                _textColor = ((Xamarin.Forms.Color)Xamarin.Forms.Application.Current.Resources["TextColor"])
                    .ToAndroid();
            }
            if (_mutedColor == default(Android.Graphics.Color))
            {
                _mutedColor = ((Xamarin.Forms.Color)Xamarin.Forms.Application.Current.Resources["MutedColor"])
                    .ToAndroid();
            }
            if (_disabledIconColor == default(Android.Graphics.Color))
            {
                _disabledIconColor =
                    ((Xamarin.Forms.Color)Xamarin.Forms.Application.Current.Resources["DisabledIconColor"])
                    .ToAndroid();
            }

            var cipherCell = item as CipherViewCell;
            _cell = convertView as AndroidCipherCell;
            if (_cell == null)
            {
                _cell = new AndroidCipherCell(context, cipherCell, _faTypeface, _miTypeface);
            }
            else
            {
                _cell.CipherViewCell.PropertyChanged -= CellPropertyChanged;
            }
            cipherCell.PropertyChanged += CellPropertyChanged;
            _cell.UpdateCell(cipherCell);
            _cell.UpdateColors(_textColor, _mutedColor, _disabledIconColor);
            return _cell;
        }

        public void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var cipherCell = sender as CipherViewCell;
            _cell.CipherViewCell = cipherCell;
            if (e.PropertyName == CipherViewCell.CipherProperty.PropertyName)
            {
                _cell.UpdateCell(cipherCell);
            }
            else if (e.PropertyName == CipherViewCell.WebsiteIconsEnabledProperty.PropertyName)
            {
                _cell.UpdateIconImage(cipherCell);
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

            var view = (context as Activity).LayoutInflater.Inflate(Resource.Layout.CipherViewCell, null);
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
            UpdateIconImage(cipherCell);

            var cipher = cipherCell.Cipher;
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
    public class IconImageView : ImageViewAsync
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

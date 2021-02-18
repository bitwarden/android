using System;
using System.ComponentModel;
using Android.App;
using Android.Content;
using Android.Graphics;
using Android.Util;
using Android.Views;
using Android.Widget;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Droid.Renderers;
using FFImageLoading.Work;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Button = Android.Widget.Button;
using Color = Android.Graphics.Color;
using View = Android.Views.View;

[assembly: ExportRenderer(typeof(SendViewCell), typeof(SendViewCellRenderer))]
namespace Bit.Droid.Renderers
{
    public class SendViewCellRenderer : ViewCellRenderer
    {
        private static Typeface _faTypeface;
        private static Typeface _miTypeface;
        private static Color _textColor;
        private static Color _mutedColor;
        private static Color _disabledIconColor;
        private static bool _usingLightTheme;

        private AndroidSendCell _cell;

        protected override View GetCellCore(Cell item, View convertView,
            ViewGroup parent, Context context)
        {
            // TODO expand beyond light/dark detection once we support custom theme switching without app restart
            var themeChanged = _usingLightTheme != ThemeManager.UsingLightTheme;
            if (_faTypeface == null)
            {
                _faTypeface = Typeface.CreateFromAsset(context.Assets, "FontAwesome.ttf");
            }
            if (_miTypeface == null)
            {
                _miTypeface = Typeface.CreateFromAsset(context.Assets, "MaterialIcons_Regular.ttf");
            }
            if (_textColor == default(Color) || themeChanged)
            {
                _textColor = ThemeManager.GetResourceColor("TextColor").ToAndroid();
            }
            if (_mutedColor == default(Color) || themeChanged)
            {
                _mutedColor = ThemeManager.GetResourceColor("MutedColor").ToAndroid();
            }
            if (_disabledIconColor == default(Color) || themeChanged)
            {
                _disabledIconColor = ThemeManager.GetResourceColor("DisabledIconColor").ToAndroid();
            }
            _usingLightTheme = ThemeManager.UsingLightTheme;

            var sendCell = item as SendViewCell;
            _cell = convertView as AndroidSendCell;
            if (_cell == null)
            {
                _cell = new AndroidSendCell(context, sendCell, _faTypeface, _miTypeface);
            }
            else
            {
                _cell.SendViewCell.PropertyChanged -= CellPropertyChanged;
            }
            sendCell.PropertyChanged += CellPropertyChanged;
            _cell.UpdateCell(sendCell);
            _cell.UpdateColors(_textColor, _mutedColor, _disabledIconColor);
            return _cell;
        }

        public void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var sendCell = sender as SendViewCell;
            _cell.SendViewCell = sendCell;
            if (e.PropertyName == SendViewCell.SendProperty.PropertyName)
            {
                _cell.UpdateCell(sendCell);
            }
        }
    }

    public class AndroidSendCell : LinearLayout, INativeElementView
    {
        private readonly Typeface _faTypeface;
        private readonly Typeface _miTypeface;

        private IScheduledWork _currentTask;

        public AndroidSendCell(Context context, SendViewCell sendView, Typeface faTypeface, Typeface miTypeface)
            : base(context)
        {
            SendViewCell = sendView;
            _faTypeface = faTypeface;
            _miTypeface = miTypeface;

            var view = (context as Activity).LayoutInflater.Inflate(Resource.Layout.SendViewCell, null);
            Icon = view.FindViewById<TextView>(Resource.Id.SendCellIcon);
            Name = view.FindViewById<TextView>(Resource.Id.SendCellName);
            SubTitle = view.FindViewById<TextView>(Resource.Id.SendCellSubTitle);
            DisabledIcon = view.FindViewById<TextView>(Resource.Id.SendCellDisabledIcon);
            HasPasswordIcon = view.FindViewById<TextView>(Resource.Id.SendCellHasPasswordIcon);
            MaxAccessCountReachedIcon = view.FindViewById<TextView>(Resource.Id.SendCellMaxAccessCountReachedIcon);
            ExpiredIcon = view.FindViewById<TextView>(Resource.Id.SendCellExpiredIcon);
            PendingDeleteIcon = view.FindViewById<TextView>(Resource.Id.SendCellPendingDeleteIcon);
            MoreButton = view.FindViewById<Button>(Resource.Id.SendCellButton);
            MoreButton.Click += MoreButton_Click;

            Icon.Typeface = _faTypeface;
            DisabledIcon.Typeface = _faTypeface;
            HasPasswordIcon.Typeface = _faTypeface;
            MaxAccessCountReachedIcon.Typeface = _faTypeface;
            ExpiredIcon.Typeface = _faTypeface;
            PendingDeleteIcon.Typeface = _faTypeface;
            MoreButton.Typeface = _miTypeface;

            var small = (float)Device.GetNamedSize(NamedSize.Small, typeof(Label));
            Icon.SetTextSize(ComplexUnitType.Pt, 10);
            Name.SetTextSize(ComplexUnitType.Sp, (float)Device.GetNamedSize(NamedSize.Medium, typeof(Label)));
            SubTitle.SetTextSize(ComplexUnitType.Sp, small);
            DisabledIcon.SetTextSize(ComplexUnitType.Sp, small);
            HasPasswordIcon.SetTextSize(ComplexUnitType.Sp, small);
            MaxAccessCountReachedIcon.SetTextSize(ComplexUnitType.Sp, small);
            ExpiredIcon.SetTextSize(ComplexUnitType.Sp, small);
            PendingDeleteIcon.SetTextSize(ComplexUnitType.Sp, small);
            MoreButton.SetTextSize(ComplexUnitType.Sp, 25);

            if (!SendViewCell.ShowOptions)
            {
                MoreButton.Visibility = ViewStates.Gone;
            }

            AddView(view);
        }

        public SendViewCell SendViewCell { get; set; }
        public Element Element => SendViewCell;

        public TextView Icon { get; set; }
        public TextView Name { get; set; }
        public TextView SubTitle { get; set; }
        public TextView DisabledIcon { get; set; }
        public TextView HasPasswordIcon { get; set; }
        public TextView MaxAccessCountReachedIcon { get; set; }
        public TextView ExpiredIcon { get; set; }
        public TextView PendingDeleteIcon { get; set; }
        public Button MoreButton { get; set; }

        public void UpdateCell(SendViewCell sendCell)
        {
            UpdateIconImage(sendCell);

            var send = sendCell.Send;
            Name.Text = send.Name;
            SubTitle.Text = send.DisplayDate;
            DisabledIcon.Visibility = send.Disabled ? ViewStates.Visible : ViewStates.Gone;
            HasPasswordIcon.Visibility = send.HasPassword ? ViewStates.Visible : ViewStates.Gone;
            MaxAccessCountReachedIcon.Visibility = send.MaxAccessCountReached ? ViewStates.Visible : ViewStates.Gone;
            ExpiredIcon.Visibility = send.Expired ? ViewStates.Visible : ViewStates.Gone;
            PendingDeleteIcon.Visibility = send.PendingDelete ? ViewStates.Visible : ViewStates.Gone;
        }

        public void UpdateIconImage(SendViewCell sendCell)
        {
            if (_currentTask != null && !_currentTask.IsCancelled && !_currentTask.IsCompleted)
            {
                _currentTask.Cancel();
            }

            var send = sendCell.Send;
            var iconImage = sendCell.GetIconImage(send);
            Icon.Text = iconImage;
        }

        public void UpdateColors(Color textColor, Color mutedColor,
            Color iconDisabledColor)
        {
            Name.SetTextColor(textColor);
            SubTitle.SetTextColor(mutedColor);
            Icon.SetTextColor(mutedColor);
            DisabledIcon.SetTextColor(mutedColor);
            HasPasswordIcon.SetTextColor(mutedColor);
            MaxAccessCountReachedIcon.SetTextColor(mutedColor);
            ExpiredIcon.SetTextColor(mutedColor);
            PendingDeleteIcon.SetTextColor(mutedColor);
            MoreButton.SetTextColor(iconDisabledColor);
        }

        private void MoreButton_Click(object sender, EventArgs e)
        {
            if (SendViewCell.ButtonCommand?.CanExecute(SendViewCell.Send) ?? false)
            {
                SendViewCell.ButtonCommand.Execute(SendViewCell.Send);
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
}

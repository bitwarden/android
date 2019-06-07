using Android.App;
using Android.Content;
using Android.Graphics;
using Android.Views;
using Android.Views.InputMethods;
using Android.Widget;
using Bit.App.Controls;
using Bit.Droid.Renderers;
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

        protected override Android.Views.View GetCellCore(Cell item, Android.Views.View convertView,
            ViewGroup parent, Context context)
        {
            if(_faTypeface == null)
            {
                _faTypeface = Typeface.CreateFromAsset(context.Assets, "FontAwesome.ttf");
            }
            if(_miTypeface == null)
            {
                _miTypeface = Typeface.CreateFromAsset(context.Assets, "MaterialIcons_Regular.ttf");
            }

            var cipherCell = item as CipherViewCell;
            if(!(convertView is AndroidCipherCell cell))
            {
                cell = new AndroidCipherCell(context, cipherCell, _faTypeface, _miTypeface);
            }
            cell.CipherViewCell.PropertyChanged += CellPropertyChanged;
            cell.CipherViewCell = cipherCell;
            cell.CipherViewCell.PropertyChanged -= CellPropertyChanged;
            cell.UpdateCell();
            return cell;
        }

        public void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var nativeCell = sender as AndroidCipherCell;
            if(e.PropertyName == CipherViewCell.CipherProperty.PropertyName)
            {
                nativeCell.UpdateCell();
            }
        }
    }

    public class AndroidCipherCell : LinearLayout, INativeElementView
    {
        private readonly Typeface _faTypeface;
        private readonly Typeface _miTypeface;

        public AndroidCipherCell(Context context, CipherViewCell cipherCell, Typeface faTypeface, Typeface miTypeface)
            : base(context)
        {
            var view = (context as Activity).LayoutInflater.Inflate(Resource.Layout.CipherViewCell, null);
            CipherViewCell = cipherCell;
            _faTypeface = faTypeface;
            _miTypeface = miTypeface;

            Name = view.FindViewById<TextView>(Resource.Id.CipherCellName);
            SubTitle = view.FindViewById<TextView>(Resource.Id.CipherCellSubTitle);
            SharedIcon = view.FindViewById<TextView>(Resource.Id.CipherCellSharedIcon);
            AttachmentsIcon = view.FindViewById<TextView>(Resource.Id.CipherCellAttachmentsIcon);
            MoreButton = view.FindViewById<Android.Widget.Button>(Resource.Id.CipherCellButton);

            SharedIcon.Typeface = _faTypeface;
            AttachmentsIcon.Typeface = _faTypeface;
            MoreButton.Typeface = _miTypeface;

            AddView(view);
        }

        public CipherViewCell CipherViewCell { get; set; }
        public Element Element => CipherViewCell;
        public TextView Name { get; set; }
        public TextView SubTitle { get; set; }
        public TextView SharedIcon { get; set; }
        public TextView AttachmentsIcon { get; set; }
        public Android.Widget.Button MoreButton { get; set; }

        public void UpdateCell()
        {
            var cipher = CipherViewCell.Cipher;
            Name.Text = cipher.Name;
            if(!string.IsNullOrWhiteSpace(cipher.SubTitle))
            {
                SubTitle.Text = cipher.SubTitle;
                SubTitle.Visibility = ViewStates.Visible;
            }
            else
            {
                SubTitle.Visibility = ViewStates.Gone;
            }
            SharedIcon.Visibility = cipher.Shared ? ViewStates.Visible : ViewStates.Gone;
            AttachmentsIcon.Visibility = cipher.HasAttachments ? ViewStates.Visible : ViewStates.Gone;
        }
    }
}

using Android.App;
using Android.Content;
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
        protected override Android.Views.View GetCellCore(Cell item, Android.Views.View convertView,
            Android.Views.ViewGroup parent, Context context)
        {
            var cipherCell = item as CipherViewCell;
            if(!(convertView is AndroidCipherCell cell))
            {
                cell = new AndroidCipherCell(context, cipherCell);
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
        public AndroidCipherCell(Context context, CipherViewCell cipherCell)
            : base(context)
        {
            var view = (context as Activity).LayoutInflater.Inflate(Resource.Layout.CipherViewCell, null);
            CipherViewCell = cipherCell;
            Title = view.FindViewById<TextView>(Resource.Id.CipherCellTitle);
            AddView(view);
        }

        public CipherViewCell CipherViewCell { get; set; }
        public Element Element => CipherViewCell;
        public TextView Title { get; set; }

        public void UpdateCell()
        {
            Title.Text = CipherViewCell.Cipher.Name;
        }
    }
}

using System.Linq;
using Xamarin.CommunityToolkit.Converters;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedCollectionView : CollectionView
    {
        public string ExtraDataForLogging { get; set; }
    }

    public class SelectionChangedEventArgsConverter : BaseNullableConverterOneWay<SelectionChangedEventArgs, object>
    {
        public override object? ConvertFrom(SelectionChangedEventArgs? value)
        {
            if (value == null)
            {
                return null;
            }
            return value?.CurrentSelection.FirstOrDefault();
        }
    }

}

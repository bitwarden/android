using Bit.App.Pages;
using Bit.Core.Models.View;

namespace Bit.App.Models
{
    public class ViewFieldViewModel : BaseViewModel
    {
        private FieldView _field;

        public ViewFieldViewModel(FieldView field)
        {
            Field = field;
        }

        public FieldView Field
        {
            get => _field;
            set => SetProperty(ref _field, value,
                additionalPropertyNames: new string[]
                {

                });
        }
    }
}

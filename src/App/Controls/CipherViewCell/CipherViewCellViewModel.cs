using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class CipherViewCellViewModel : ExtendedViewModel
    {
        private CipherView _cipher;

        public CipherViewCellViewModel(CipherView cipherView)
        {
            Cipher = cipherView;
        }

        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value);
        }
    }
}

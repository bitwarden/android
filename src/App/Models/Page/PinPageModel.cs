using System.ComponentModel;

namespace Bit.App.Models.Page
{
    public class PinPageModel : INotifyPropertyChanged
    {
        private string _pin = string.Empty;

        public event PropertyChangedEventHandler PropertyChanged;

        public string LabelText
        {
            get
            {
                var newText = string.Empty;
                for(int i = 0; i < 4; i++)
                {
                    newText += _pin.Length <= i ? "- " : "• ";
                }

                return newText.TrimEnd();
            }
        }

        public string PIN
        {
            get { return _pin; }
            set
            {
                _pin = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(PIN)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LabelText)));
            }
        }
    }
}

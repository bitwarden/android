using System;
using System.ComponentModel;

namespace Bit.App.Models.Page
{
    public class PasswordGeneratorPageModel : INotifyPropertyChanged
    {
        private string _password = " ";
        private string _length;

        public PasswordGeneratorPageModel() { }

        public event PropertyChangedEventHandler PropertyChanged;

        public string Password
        {
            get { return _password; }
            set
            {
                _password = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Password)));
            }
        }

        public string Length
        {
            get { return _length; }
            set
            {
                _length = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Length)));
            }
        }
    }
}

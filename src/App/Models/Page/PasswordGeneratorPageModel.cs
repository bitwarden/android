using System;
using System.ComponentModel;
using Bit.App.Utilities;
using Xamarin.Forms;

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
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(FormattedPassword)));
            }
        }

        public FormattedString FormattedPassword
        {
            get { return PasswordFormatter.FormatPassword(_password); }
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

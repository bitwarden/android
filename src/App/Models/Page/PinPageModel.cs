using System;
using System.Collections.Generic;
using System.ComponentModel;

namespace Bit.App.Models.Page
{
    public class PinPageModel : INotifyPropertyChanged
    {
        private string _labelText;
        private List<string> _pin;

        public PinPageModel()
        {
            LabelText = "_  _  _  _";
            PIN = new List<string>();
        }

        public event PropertyChangedEventHandler PropertyChanged;

        public string LabelText
        {
            get { return _labelText; }
            set
            {
                _labelText = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LabelText)));
            }
        }
        public List<string> PIN
        {
            get { return _pin; }
            set
            {
                _pin = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(PIN)));
            }
        }
    }
}

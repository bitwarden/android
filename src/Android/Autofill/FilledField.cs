using System.Collections.Generic;
using static Android.App.Assist.AssistStructure;

namespace Bit.Android.Autofill
{
    public class FilledField
    {
        private IEnumerable<string> _hints = null;

        public FilledField() { }

        public FilledField(ViewNode node)
        {
            _hints = AutofillHelpers.FilterForSupportedHints(node.GetAutofillHints());
            var autofillValue = node.AutofillValue;
            if(autofillValue == null)
            {
                return;
            }

            if(autofillValue.IsList)
            {
                var autofillOptions = node.GetAutofillOptions();
                int index = autofillValue.ListValue;
                if(autofillOptions != null && autofillOptions.Length > 0)
                {
                    TextValue = autofillOptions[index];
                }
            }
            else if(autofillValue.IsDate)
            {
                DateValue = autofillValue.DateValue;
            }
            else if(autofillValue.IsText)
            {
                // Using toString of AutofillValue.getTextValue in order to save it to
                // SharedPreferences.
                TextValue = autofillValue.TextValue;
            }
        }

        public string TextValue { get; set; }
        public long? DateValue { get; set; }
        public bool? ToggleValue { get; set; }

        public IEnumerable<string> GetHints()
        {
            return _hints;
        }

        public bool IsNull()
        {
            return TextValue == null && DateValue == null && ToggleValue == null;
        }

        public override bool Equals(object o)
        {
            if(this == o)
            {
                return true;
            }

            if(o == null || GetType() != o.GetType())
            {
                return false;
            }

            var that = o as FilledField;
            if(TextValue != null ? !TextValue.Equals(that.TextValue) : that.TextValue != null)
            {
                return false;
            }

            if(DateValue != null ? !DateValue.Equals(that.DateValue) : that.DateValue != null)
            {
                return false;
            }

            return ToggleValue != null ? ToggleValue.Equals(that.ToggleValue) : that.ToggleValue == null;
        }

        public override int GetHashCode()
        {
            var result = TextValue != null ? TextValue.GetHashCode() : 0;
            result = 31 * result + (DateValue != null ? DateValue.GetHashCode() : 0);
            result = 31 * result + (ToggleValue != null ? ToggleValue.GetHashCode() : 0);
            return result;
        }
    }
}
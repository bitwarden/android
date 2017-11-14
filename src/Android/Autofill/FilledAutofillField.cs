using static Android.App.Assist.AssistStructure;

namespace Bit.Android.Autofill
{
    public class FilledAutofillField
    {
        /**
         * Does not need to be serialized into persistent storage, so it's not exposed.
         */
        private string[] _autofillHints = null;

        public FilledAutofillField() { }

        public FilledAutofillField(ViewNode viewNode)
        {
            _autofillHints = AutofillHelper.FilterForSupportedHints(viewNode.GetAutofillHints());
            var autofillValue = viewNode.AutofillValue;
            if(autofillValue != null)
            {
                if(autofillValue.IsList)
                {
                    var autofillOptions = viewNode.GetAutofillOptions();
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
        }

        public string TextValue { get; set; }
        public long? DateValue { get; set; }
        public bool? ToggleValue { get; set; }

        public string[] GetAutofillHints()
        {
            return _autofillHints;
        }

        public bool IsNull()
        {
            return TextValue == null && DateValue == null && ToggleValue == null;
        }

        public override bool Equals(object o)
        {
            if(this == o)
                return true;

            if(o == null || GetType() != o.GetType())
                return false;

            var that = (FilledAutofillField)o;
            if(TextValue != null ? !TextValue.Equals(that.TextValue) : that.TextValue != null)
                return false;
            if(DateValue != null ? !DateValue.Equals(that.DateValue) : that.DateValue != null)
                return false;

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
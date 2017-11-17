using System.Collections.Generic;
using static Android.App.Assist.AssistStructure;

namespace Bit.Android.Autofill
{
    public class FilledField
    {
        public FilledField() { }

        public FilledField(ViewNode node)
        {
            Hints = AutofillHelpers.FilterForSupportedHints(node.GetAutofillHints());

            if(node.AutofillValue == null)
            {
                return;
            }

            if(node.AutofillValue.IsList)
            {
                var autofillOptions = node.GetAutofillOptions();
                if(autofillOptions != null && autofillOptions.Length > 0)
                {
                    TextValue = autofillOptions[node.AutofillValue.ListValue];
                }
            }
            else if(node.AutofillValue.IsDate)
            {
                DateValue = node.AutofillValue.DateValue;
            }
            else if(node.AutofillValue.IsText)
            {
                TextValue = node.AutofillValue.TextValue;
            }
        }

        public string TextValue { get; set; }
        public long? DateValue { get; set; }
        public bool? ToggleValue { get; set; }
        public List<string> Hints { get; set; }

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

            var field = o as FilledField;
            if(TextValue != null ? !TextValue.Equals(field.TextValue) : field.TextValue != null)
            {
                return false;
            }

            if(DateValue != null ? !DateValue.Equals(field.DateValue) : field.DateValue != null)
            {
                return false;
            }

            return ToggleValue != null ? ToggleValue.Equals(field.ToggleValue) : field.ToggleValue == null;
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
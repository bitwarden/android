using System;
using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;
using System.Linq;
using Android.Text;

namespace Bit.Android.Autofill
{
    public class FilledFieldCollection : IFilledItem
    {
        public FilledFieldCollection()
            : this(null, new Dictionary<string, FilledField>())
        { }

        public FilledFieldCollection(string datasetName, IDictionary<string, FilledField> hintMap)
        {
            HintToFieldMap = hintMap;
            Name = datasetName;
            Subtitle = "username";
        }

        public IDictionary<string, FilledField> HintToFieldMap { get; private set; }
        public string Name { get; set; }
        public string Subtitle { get; set; }

        public void Add(FilledField filledField)
        {
            if(filledField == null)
            {
                throw new ArgumentNullException(nameof(filledField));
            }

            foreach(var hint in filledField.Hints)
            {
                HintToFieldMap.Add(hint, filledField);
            }
        }

        public bool ApplyToFields(FieldCollection fieldCollection, Dataset.Builder datasetBuilder)
        {
            var setValue = false;
            foreach(var hint in fieldCollection.Hints)
            {
                if(!fieldCollection.HintToFieldsMap.ContainsKey(hint))
                {
                    continue;
                }

                var fillableFields = fieldCollection.HintToFieldsMap[hint];
                for(var i = 0; i < fillableFields.Count; i++)
                {
                    if(!HintToFieldMap.ContainsKey(hint))
                    {
                        continue;
                    }

                    var field = fillableFields[i];
                    var filledField = HintToFieldMap[hint];

                    switch(field.AutofillType)
                    {
                        case AutofillType.List:
                            int listValue = field.GetAutofillOptionIndex(filledField.TextValue);
                            if(listValue != -1)
                            {
                                datasetBuilder.SetValue(field.AutofillId, AutofillValue.ForList(listValue));
                                setValue = true;
                            }
                            break;
                        case AutofillType.Date:
                            var dateValue = filledField.DateValue;
                            if(dateValue != null)
                            {
                                datasetBuilder.SetValue(field.AutofillId, AutofillValue.ForDate(dateValue.Value));
                                setValue = true;
                            }
                            break;
                        case AutofillType.Text:
                            var textValue = filledField.TextValue;
                            if(textValue != null)
                            {
                                datasetBuilder.SetValue(field.AutofillId, AutofillValue.ForText(textValue));
                                setValue = true;
                            }
                            break;
                        case AutofillType.Toggle:
                            var toggleValue = filledField.ToggleValue;
                            if(toggleValue != null)
                            {
                                datasetBuilder.SetValue(field.AutofillId, AutofillValue.ForToggle(toggleValue.Value));
                                setValue = true;
                            }
                            break;
                        case AutofillType.None:
                        default:
                            break;
                    }
                }
            }

            return setValue;
        }

        public bool HelpsWithHints(List<String> autofillHints)
        {
            return autofillHints.Any(h => HintToFieldMap.ContainsKey(h) && !HintToFieldMap[h].IsNull());
        }
    }
}
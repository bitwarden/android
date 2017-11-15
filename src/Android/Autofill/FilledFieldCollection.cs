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

        /**
         * Adds a {@code FilledAutofillField} to the collection, indexed by all of its hints.
         */
        public void Add(FilledField filledAutofillField)
        {
            if(filledAutofillField == null)
            {
                throw new ArgumentNullException(nameof(filledAutofillField));
            }

            var autofillHints = filledAutofillField.GetHints();
            foreach(var hint in autofillHints)
            {
                HintToFieldMap.Add(hint, filledAutofillField);
            }
        }

        /**
         * Populates a {@link Dataset.Builder} with appropriate values for each {@link AutofillId}
         * in a {@code AutofillFieldMetadataCollection}.
         *
         * In other words, it constructs an autofill
         * {@link Dataset.Builder} by applying saved values (from this {@code FilledAutofillFieldCollection})
         * to Views specified in a {@code AutofillFieldMetadataCollection}, which represents the current
         * page the user is on.
         */
        public bool ApplyToFields(FieldCollection fieldCollection, Dataset.Builder datasetBuilder)
        {
            var setValueAtLeastOnce = false;
            var allHints = fieldCollection.Hints;
            for(var hintIndex = 0; hintIndex < allHints.Count; hintIndex++)
            {
                var hint = allHints[hintIndex];
                if(!fieldCollection.HintToFieldsMap.ContainsKey(hint))
                {
                    continue;
                }

                var fillableAutofillFields = fieldCollection.HintToFieldsMap[hint];
                for(var autofillFieldIndex = 0; autofillFieldIndex < fillableAutofillFields.Count; autofillFieldIndex++)
                {
                    if(!HintToFieldMap.ContainsKey(hint))
                    {
                        continue;
                    }

                    var filledField = HintToFieldMap[hint];
                    var fieldMetadata = fillableAutofillFields[autofillFieldIndex];
                    var autofillId = fieldMetadata.AutofillId;
                    switch(fieldMetadata.AutofillType)
                    {
                        case AutofillType.List:
                            int listValue = fieldMetadata.GetAutofillOptionIndex(filledField.TextValue);
                            if(listValue != -1)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForList(listValue));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Date:
                            var dateValue = filledField.DateValue;
                            if(dateValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForDate(dateValue.Value));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Text:
                            var textValue = filledField.TextValue;
                            if(textValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForText(textValue));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Toggle:
                            var toggleValue = filledField.ToggleValue;
                            if(toggleValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForToggle(toggleValue.Value));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.None:
                        default:
                            break;
                    }
                }
            }

            if(!setValueAtLeastOnce)
            {
                var password = fieldCollection.Fields.FirstOrDefault(f => f.InputType == InputTypes.TextVariationPassword);
               // datasetBuilder.SetValue(password.AutofillId, AutofillValue.ForText());
                if(password != null)
                {
                    var username = fieldCollection.Fields.TakeWhile(f => f.Id != password.Id).LastOrDefault();
                }
            }

            return setValueAtLeastOnce;
        }

        /**
         * Takes in a list of autofill hints (`autofillHints`), usually associated with a View or set of
         * Views. Returns whether any of the filled fields on the page have at least 1 of these
         * `autofillHint`s.
         */
        public bool HelpsWithHints(List<String> autofillHints)
        {
            return autofillHints.Any(h => HintToFieldMap.ContainsKey(h) && !HintToFieldMap[h].IsNull());
        }
    }
}
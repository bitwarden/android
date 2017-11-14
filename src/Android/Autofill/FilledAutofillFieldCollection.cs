using System;
using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;

namespace Bit.Android.Autofill
{
    public class FilledAutofillFieldCollection
    {
        public FilledAutofillFieldCollection()
            : this(null, new Dictionary<string, FilledAutofillField>())
        {
        }

        public FilledAutofillFieldCollection(string datasetName, IDictionary<string, FilledAutofillField> hintMap)
        {
            HintMap = hintMap;
            DatasetName = datasetName;
        }

        public IDictionary<string, FilledAutofillField> HintMap { get; private set; }
        public string DatasetName { get; set; }

        /**
         * Adds a {@code FilledAutofillField} to the collection, indexed by all of its hints.
         */
        public void Add(FilledAutofillField filledAutofillField)
        {
            if(filledAutofillField == null)
            {
                throw new ArgumentNullException(nameof(filledAutofillField));
            }

            var autofillHints = filledAutofillField.GetAutofillHints();
            foreach(var hint in autofillHints)
            {
                HintMap.Add(hint, filledAutofillField);
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
        public bool ApplyToFields(AutofillFieldMetadataCollection autofillFieldMetadataCollection,
            Dataset.Builder datasetBuilder)
        {
            var setValueAtLeastOnce = false;
            var allHints = autofillFieldMetadataCollection.AutofillHints;
            for(var hintIndex = 0; hintIndex < allHints.Count; hintIndex++)
            {
                var hint = allHints[hintIndex];
                if(!autofillFieldMetadataCollection.AutofillHintsToFieldsMap.ContainsKey(hint))
                {
                    continue;
                }

                var fillableAutofillFields = autofillFieldMetadataCollection.AutofillHintsToFieldsMap[hint];
                for(var autofillFieldIndex = 0; autofillFieldIndex < fillableAutofillFields.Count; autofillFieldIndex++)
                {
                    if(!HintMap.ContainsKey(hint))
                    {
                        continue;
                    }

                    var filledAutofillField = HintMap[hint];
                    var autofillFieldMetadata = fillableAutofillFields[autofillFieldIndex];
                    var autofillId = autofillFieldMetadata.AutofillId;
                    var autofillType = autofillFieldMetadata.AutofillType;
                    switch(autofillType)
                    {
                        case AutofillType.List:
                            int listValue = autofillFieldMetadata.GetAutofillOptionIndex(filledAutofillField.TextValue);
                            if(listValue != -1)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForList(listValue));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Date:
                            var dateValue = filledAutofillField.DateValue;
                            if(dateValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForDate(dateValue.Value));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Text:
                            var textValue = filledAutofillField.TextValue;
                            if(textValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForText(textValue));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Toggle:
                            var toggleValue = filledAutofillField.ToggleValue;
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

            return setValueAtLeastOnce;
        }

        /**
         * Takes in a list of autofill hints (`autofillHints`), usually associated with a View or set of
         * Views. Returns whether any of the filled fields on the page have at least 1 of these
         * `autofillHint`s.
         */
        public bool HelpsWithHints(List<String> autofillHints)
        {
            for(var i = 0; i < autofillHints.Count; i++)
            {
                if(HintMap.ContainsKey(autofillHints[i]) && !HintMap[autofillHints[i]].IsNull())
                {
                    return true;
                }
            }

            return false;
        }
    }
}